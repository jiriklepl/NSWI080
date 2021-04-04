// Standard library headers
#include <memory>
#include <functional>
#include <iostream>
#include <string>
#include <sstream>
#include <atomic>

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

using namespace apache::thrift;
using namespace apache::thrift::transport;
using namespace apache::thrift::protocol;
using namespace apache::thrift::server;

// Implementation of the Login service
class LoginHandler: public LoginIf{

    // Each connection gets assigned an id
    // That allows us to see how for each connection, a new handler is used
    std::size_t connectionId;
    bool loggedOn;

public:
    LoginHandler(std::size_t connectionId) :
        connectionId(connectionId), loggedOn(false) {}

    // Implementation of logIn
    void logIn(const std::string& userName, const std::int32_t key) override {
        std::int32_t expected_key = std::hash<std::string>{}(userName);
        if (key != expected_key) {
            InvalidKeyException e;
            e.invalidKey = key;
            e.expectedKey = expected_key;
            throw std::move(e);
        } else {
            loggedOn = true;
        }
    }

    // Implementation of logOut
    void logOut() override {
        loggedOn = false;
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
        std::size_t connectionId = assignId();
        // Create a loginHandler for the Login service
        auto loginHandler = std::make_shared<LoginHandler>(connectionId);
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