// Standard library headers
#include <memory>
#include <functional>
#include <iostream>
#include <string>
#include <sstream>
#include <string_view>
#include <atomic>
#include <map>
#include <set>

// Thrift headers
#include <thrift/protocol/TProtocol.h>
#include <thrift/protocol/TBinaryProtocol.h>
#include <thrift/protocol/TMultiplexedProtocol.h>
#include <thrift/transport/TSocket.h>
#include <thrift/transport/TTransportUtils.h>
#include <thrift/server/TServer.h>
#include <thrift/server/TThreadedServer.h>
#include <thrift/processor/TMultiplexedProcessor.h>
#include <thrift/TProcessor.h>
#include <thrift/Thrift.h>

// Generated headers
#include "gen-cpp/Login.h"
#include "gen-cpp/Search.h"

using namespace apache::thrift;
using namespace apache::thrift::transport;
using namespace apache::thrift::protocol;
using namespace apache::thrift::server;

struct UserData {
    UserData(std::size_t connectionId) :
        connectionId(connectionId), loggedOn(false), fetched() {}
    std::size_t connectionId;
    bool loggedOn;
    std::map<std::string, std::set<std::string>> fetched;
};

// Implementation of the Login service
class LoginHandler: public LoginIf {
    // Each connection gets assigned an id
    // That allows us to see how for each connection, a new handler is used
    std::shared_ptr<UserData> user_data;

public:
    LoginHandler(std::shared_ptr<UserData> user_data) :
        user_data(std::move(user_data)) {}

    // Implementation of logIn
    void logIn(const std::string& userName, const std::int32_t key) override {
        std::int32_t expected_key = std::hash<std::string>{}(userName);

        if (user_data->loggedOn) {
            ProtocolException e;
            e.message = "Already logged on";

            throw e;
        }

        if (key != expected_key) {
            InvalidKeyException e;
            e.invalidKey = key;
            e.expectedKey = expected_key;

            throw e;
        } else {
            user_data->loggedOn = true;
        }
    }

    // Implementation of logOut
    void logOut() override {
        if (!user_data->loggedOn) {
            ProtocolException e;
            e.message = "Not logged on";

            throw e;
        }

        user_data->loggedOn = false;
    }
};

class SearchHandler : public SearchIf {
    // Each connection gets assigned an id
    // That allows us to see how for each connection, a new handler is used
    std::shared_ptr<UserData> user_data;
    std::map<std::string, std::function<Item()>> item_factory = {
        { "itemA", []() {
            // TODO: add things to fetched
            Item item;
            item.itemA.fieldA = rand();
            std::size_t i = rand() & 0x7;
            while (i--)
                item.itemA.fieldB.push_back((short)rand());
            item.itemA.fieldC = rand();
            return item;
        }},
        { "itemB", [this]() {
            Item item;
            {
                std::stringstream ss;
                ss << rand();
                user_data->fetched.try_emplace("fieldA", std::set<std::string>()).first->second.emplace(ss.str());
                item.itemB.fieldA = ss.str();
            }
            {
                std::size_t i = rand() & 0x7;
                while (i--) {
                    std::stringstream ss;
                    ss << rand();
                    user_data->fetched.try_emplace("fieldB", std::set<std::string>()).first->second.emplace(ss.str());
                    item.itemB.fieldB.emplace(ss.str());
                }
            }
            {
                std::size_t i = rand() & 0x7;
                while (i--) {
                    std::stringstream ss;
                    ss << rand();
                    user_data->fetched.try_emplace("fieldC", std::set<std::string>()).first->second.emplace(ss.str());
                    item.itemB.fieldC.push_back(ss.str());
                }
            }
            return item;
        }},
        { "itemC", []() {
            Item item;
            item.itemC.fieldA = (rand() & 1) == 1;
            return item;
        }},
    };

    struct Fetcher {
        Fetcher() = default;
        Fetcher(std::size_t i, std::size_t j, std::uint32_t limit, const std::string &query) :
            i(i), j(j), limit(limit), query(query) {}
        std::size_t i = 0, j;
        std::uint32_t limit;
        std::string query;
    };

    std::unique_ptr<Fetcher> fetcher = nullptr;

public:
    SearchHandler(std::shared_ptr<UserData> user_data) :
        user_data(std::move(user_data)) {}

    void search(SearchState& _return, const std::string& query, const int32_t limit) override {
        // Your implementation goes here

        if (fetcher != nullptr) {
            ProtocolException e;
            e.message = "Searching already initiated";

            throw e;
        }
        
        fetcher = std::make_unique<Fetcher>(0, 0, limit, query);
    }

    void fetch(FetchResult& _return, const SearchState& state) override {
        if (fetcher == nullptr) {
            ProtocolException e;
            e.message = "Searching not initiated";

            throw e;
        }

        if (fetcher->limit == 0) {
            _return.state = FetchState::ENDED;
            fetcher = nullptr;
            return;
        } else if ((rand() & 3) == 0) {
            _return.state = FetchState::PENDING;
            return;
        }

        while (true) {
            if ((fetcher->j = fetcher->query.find(',', fetcher->i)) == std::string::npos) {
                fetcher->i = 0;
                continue;
            }

            auto it = item_factory.find(fetcher->query.substr(fetcher->i, fetcher->j - fetcher->i));

            if (it != item_factory.end()) {
                _return.item = it->second();
            }

            if ((rand() & 3) == 0)
                fetcher->i = fetcher->j + 1;

            if ((rand() & 3) == 0)
                --fetcher->limit;

            break;
        }
    }

};

// This factory creates a new handler for each conection
class PerConnectionProcessorFactory: public TProcessorFactory{
    // We assign each handler an id
    std::atomic<std::size_t> connectionIdCounter;

public:
    PerConnectionProcessorFactory(): connectionIdCounter(0) {}

    // The counter is incremented for each connection
    std::size_t assignId() {
        return ++connectionIdCounter;
    }

    // This metod is called for each connection
    std::shared_ptr<TProcessor> getProcessor(const TConnectionInfo& connInfo) override {
        // Assign a new id to this connection
        auto user_data = std::make_shared<UserData>(assignId());
        // Create a loginHandler for the Login service
        auto loginHandler = std::make_shared<LoginHandler>(user_data);
        // Create a processor for the Login service
        auto loginProcessor = std::make_shared<LoginProcessor>(loginHandler);
        // Add the loginProcessor to a multiplexed loginProcessor
        // This allows extending this server by adding more services
        auto muxProcessor = std::make_shared<TMultiplexedProcessor>();
        muxProcessor->registerProcessor("Login", loginProcessor);
        // Use the multiplexed processor
        return muxProcessor;
    }
};

int main(){
    
    try{
        // Accept connections on a TCP socket
        auto serverTransport = std::make_shared<TServerSocket>(5000);
        // Use buffering
        auto transportFactory = std::make_shared<TBufferedTransportFactory>();
        // Use a binary protocol to serialize data
        auto protocolFactory = std::make_shared<TBinaryProtocolFactory>();
        // Use a processor factory to create a processor per connection
        auto processorFactory = std::make_shared<PerConnectionProcessorFactory>();

        // Start the server
        TThreadedServer server(processorFactory, serverTransport, transportFactory, protocolFactory);
        server.serve();
    }
    catch (TException& tx) {
        std::cerr << "ERROR: " << tx.what() << std::endl;
    }

}