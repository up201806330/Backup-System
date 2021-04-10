# Delete enhancement
> *If a peer that backs up some chunks of the file is not running at the time the initiator peer sends a DELETE message for that file, the space used by these chunks will never be reclaimed. Can you think of a change to the protocol, possibly including additional messages, that would allow to reclaim storage space even in that event?*

Since every initiator stores the peers that are storing its backed up chunks,
there is a possbility to verify what peers actually were able to delete these chunks,
using a reply message.
In our case, `1.1 DELETED <SenderID> <ChunkID>` serves as this verification.

Simultaneously, an updated version of peer would broadcast the chunks it currently has,
so that all initiators can check if any of these are supposed to have been deleted,
and promptly send a new `DELETE` message.