# Solution

## Overall design

The overall design does not modify the initial template given by the assignment and following this template left almost no space for meaningful architectural decisions. And thus, the following section will discuss only implementation details and design decision for very low-abstraction parts of the application.

## Design decisions

The state of the server-side part of the application is required to use multiple maps indexed by user names and document names. I chose 1 for the users and 1 for the documents as this design seemed (without a deeper research supported by experiments and test) as the most natural.

The assignment explicitly states that the lists of favorite documents (for each user) and the lists of comments (for each document) will be small. If this wouldn't be the case, we might consider storing those separately.

- **Users** map: stores the `User` data

    - `selectedDocument : DocumentData`
        
        - cached selected Document (see the [Loading](#Loading) section)
    
    - `favorites : ArrayList<String>`

        - the list of favorite documents (the choice of ArrayList is motivated by it being the most standard collection and by that it is Serializable and more efficient than other standard alternatives)
        - the design is based on an decision that it is perfectly valid to have non-unique entries to the favorites list

    - `lastFavorite : int`

        - used for cycling through favorites

- **Documents** map: stores the document contents and their meta data in a `DocumentData` structure

    - `name : String`

        - the name of the document

	- `document : Document`

        - the contents of the document

	- `views : int`

        - the number of accesses of the document

	- `comments : ArrayList<String>`

        - the list of the comments for the document (see *favorites* in the *Users* map above for the reasoning behind the choice of this particular type)

## Loading

The assignment explicitly states that the "s" and "n" commands load the documents while the "i" command just prints information about the currently selected document. This lead me to the decision that for, each user, the servers should cache the currently selected document in the state it was selected as the assignment explicitly requires this behavior. This means that after selecting a document (through either an "s" command, or an "n" command) the user will see all preceding comments and accesses, but not those which follow the command that issued a selection. The current state of accesses and comments is updated after the very next "s" or "n" command. This specification quite limits the design in resource management: without this narrow requirement on the behavior of these commands ("s", "n", and "i") it would be possible to "load" the last version which is in the *Documents* cache.

## Configuration for Haselcast

The *Documents* cache does not require persistency between multiple runtimes (it opposes the nature of cache) and in a production version of the application we would have some clear limits on the size of the cached document data (content and meta). So, for demonstration, I chose a maximum size of 16 kB with the LRU strategy (this strategy behaves quite well in the most used scenarios and it can be rigorously proven to be as efficient, disregarding a constant, as a strategy with an omniscient oracle).

For the *Users* map, I chose, for the demonstration, the implicitly set backup (a better suited number could be decided after some testing and after researching common requirements of the users).
