# Solution

## Steps according to "Your Task" List:

1. I decided to use a MapMessage message as we have a map of Goods, and also, I added a CLIENT_NAME_PROPERTY property with the value being equal to the client's name  as I think it would be useful later for identification.  Then I created a replyQueue named, again, after the client's name for the very same reason. For the message to the bank ~I decided for a simple String Message~(this is implied by the Bank implementation). The assignment doesn't specify the response message type, but I found out the implementation does specify it. So there are no further meaningful decisions except a 10s timeout if the bank or the connection dies for some reason.

2. OK. I just found out that everything I just coded according to the assignment is already in the Client.connect method...

I decided to change my approach here. Now I lookup all TODOs and act according to those (this, I assume, is the intended approach).

## The TODOs approach:

First, I deleted all my code. Btw. thank you for the comments that explain what to do. Now, the assignment seems much easier and I can even learn from it.

### First TODO (connect)

1. Here I could reuse some of my deleted code (see that part for any details regarding this)
2. Since this step, I decided to make all variables that seem important fields of the client. And all string literals static final fields

### Second TODO (publishGoodsList)

Here I opted for the MapMessage as I explain in the first step.

I looked at my original code and I decided it works here as well.

### Third TODO (buy)

Again, I put all string literals as static finals and I opted for a MapMessage (as I think it is the most suitable solution to the problem). For the confirmation, I went with a simple TextMessage.

### Fourth TODO (processOffer)

I added offerTopic to publishGoodsList (the assignment didn't mention it and the sender can have assigned destinations -> which it doesn't, after some inspection). Other than that, the implementation of this TODO was fairly straight-forward with no meaningful decisions made.

### Fifth TODO (processSale)

I changed the Map.get check to Map.remove (as it returns NULL on failure). Then I decided to change reservedGoods to map reservers to goods as this allows us to remember what the reservers reserved and the name in the map is kinda redundant. Other thn that, there were no more meaningful decisions

### Sixth TODO (processBankReport)

No meaningful decisions here

### Seventh TODO (Bank)

I decided to add REPORT_TYPE_CANCELED, and then accountsBalances map mapping accounts to balances (everyone gets 10'000 upon account creation). I decided to check whether both accounts exist and whether the client has enough money and then I decided to update usign the CompareExchange-style version of replace (because we are a bank)

## Debugging

I had to change goods to just prices in publishGoodsList and processOffer. This makes the overal design even better as there was a little bit of unnecessary redundancy there.

I added check() with an user option "c", which uses similar mechanism to the one with "new account", but with the replyQueue and everything was changed accordingly.

I added fetchOffers() with an user option "f" so the user can request offers from other instances, this command is run automatically upon startup.
