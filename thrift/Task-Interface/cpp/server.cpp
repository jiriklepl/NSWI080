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
#include "gen-cpp/Reports.h"

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
        { "itemA", [this]() {
            Item item;

            item.__isset.itemA = true;

            {
                auto s = std::to_string(item.itemA.fieldA = rand());
                user_data->fetched.try_emplace("fieldA", std::set<std::string>()).first->second.emplace(s);
            }
            
            {
                std::size_t i = rand() & 0x7;
                std::stringstream ss;
                while (i--) {
                    auto num = (short)rand();
                    ss << num;
                    if (i) ss << ',';
                    item.itemA.fieldB.push_back(num);
                }
                user_data->fetched.try_emplace("fieldB", std::set<std::string>()).first->second.emplace(ss.str());
            }
            
            {
                auto s = std::to_string(item.itemA.fieldC = rand());
                user_data->fetched.try_emplace("fieldC", std::set<std::string>()).first->second.emplace(s);
            }
            
            return item;
        }},
        { "itemB", [this]() {
            Item item;
            item.__isset.itemB = true;
            {
                auto s = std::to_string(rand());
                user_data->fetched.try_emplace("fieldA", std::set<std::string>()).first->second.emplace(s);
                item.itemB.fieldA = s;
            }
            {
                std::size_t i = rand() & 0x7;
                while (i--) {
                    auto s = std::to_string(rand());
                    item.itemB.fieldB.emplace(s);
                }
                std::stringstream ss;
                for (auto &&s : item.itemB.fieldB) {
                    if (++i) ss << ',';
                    ss << s;
                }
                user_data->fetched.try_emplace("fieldB", std::set<std::string>()).first->second.emplace(ss.str());
            }
            {
                std::size_t i = rand() & 0x7;
                std::stringstream ss;
                if (i != 0)
                    item.itemB.__isset.fieldC = true;
                while (i--) {
                    auto s = std::to_string(rand());
                    ss << s;
                    if (i) ss << ',';
                    item.itemB.fieldC.push_back(s);
                }
                if (item.itemB.__isset.fieldC)
                    user_data->fetched.try_emplace("fieldC", std::set<std::string>()).first->second.emplace(ss.str());
            }
            return item;
        }},
        { "itemC", [this]() {
            Item item;
            item.__isset.itemC = true;
            auto s = std::to_string(item.itemC.fieldA = (rand() & 1) == 1);
            user_data->fetched.try_emplace("fieldA", std::set<std::string>()).first->second.emplace(s);
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
        if (!user_data->loggedOn) {
            ProtocolException e;
            e.message = "Not logged on";

            throw e;
        }

        if (fetcher != nullptr) {
            ProtocolException e;
            e.message = "Searching already initiated";

            throw e;
        }
        
        fetcher = std::make_unique<Fetcher>(0, 0, limit, query);
    }

    void fetch(FetchResult& _return, const SearchState& state) override {
        if (!user_data->loggedOn) {
            ProtocolException e;
            e.message = "Not logged on";

            throw e;
        }

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

        _return.state = FetchState::ITEMS;

        fetcher->j = fetcher->query.find(',', fetcher->i);

        auto it = (fetcher->j != fetcher->query.npos)
            ? item_factory.find(fetcher->query.substr(fetcher->i, fetcher->j - fetcher->i))
            : item_factory.find(fetcher->query.substr(fetcher->i));

        if (it != item_factory.end()) {
            _return.__set_item(it->second());
        } else {
            _return.state = FetchState::PENDING;
        }

        if ((rand() & 3) == 0)
            fetcher->i = (fetcher->j != fetcher->query.npos)
                ? fetcher->j + 1
                : 0;

        if ((rand() & 3) == 0)
            --fetcher->limit;
    }
};

class ReportsHandler : public ReportsIf {
    std::shared_ptr<UserData> user_data;

public:
    ReportsHandler(std::shared_ptr<UserData> user_data) :
        user_data(user_data) {}

    bool saveReport(const Report& report) override {
        if (!user_data->loggedOn) {
            ProtocolException e;
            e.message = "Not logged on";

            throw e;
        }

        return report == user_data->fetched;
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

        // Create a loginHandler for the Search service
        auto searchHandler = std::make_shared<SearchHandler>(user_data);
        // Create a processor for the Search service
        auto searchProcessor = std::make_shared<SearchProcessor>(searchHandler);

        // Create a loginHandler for the Reports service
        auto reportsHandler = std::make_shared<ReportsHandler>(user_data);
        // Create a processor for the Reports service
        auto reportsProcessor = std::make_shared<ReportsProcessor>(reportsHandler);

        // Add the loginProcessor to a multiplexed muxProcessor
        // Add the searchProcessor to a multiplexed muxProcessor
        // Add the reportsProcessor to a multiplexed muxProcessor
        // This allows extending this server by adding more services
        auto muxProcessor = std::make_shared<TMultiplexedProcessor>();
        muxProcessor->registerProcessor("Login", loginProcessor);
        muxProcessor->registerProcessor("Search", searchProcessor);
        muxProcessor->registerProcessor("Reports", reportsProcessor);
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