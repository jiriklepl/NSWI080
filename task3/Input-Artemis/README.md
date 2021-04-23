# Solution

1. I decided to use a MapMessage message as we have a map of Goods, and also, I added a CLIENT_NAME_PROPERTY property with the value being equal to the client's name  as I think it would be useful later for identification.  Then I created a replyQueue named, again, after the client's name for the very same reason. For the message to the bank ~I decided for a simple String Message~(this is implied by the Bank implementation). The assignment doesn't specify the response message type, but I found out the implementation does specify it. So there are no further meaningful decisions except a 10s timeout if the bank or the connection dies for some reason.

2. OK. I Just FOumnd out that everything I just coded according to the assignment is already in the Client.connect method.......


