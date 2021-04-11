# Backup enhancement
> *This scheme can deplete the backup space rather rapidly, and cause too much activity on the nodes once that space is full. Can you think of an alternative scheme that ensures the desired replication degree, avoids these problems, and, nevertheless, can interoperate with peers that execute the chunk backup protocol described above?*

The easiest way to avoid chunks being replicated way more times than their desired replication degree
is by using the approach described in the restore protocol to avoid flooding the host with chunk messages:
upon receiving a PUTCHUNK message, the peer would wait *a random time uniformly distributed between 0 and 400 ms* 
and only sending a STORED reply if by that time the perceived replication degree is still lower than the desired one.
For this we had to add another data structure to each peer, a *concurrent set that stores all chunks in the system*, 
that is, both the ones stored locally, and the ones backed up by other peers.
This enhancement helps decrease the number of unnecessary backups, at the cost of extra time spent on the operation.

# Delete enhancement
> *If a peer that backs up some chunks of the file is not running at the time the initiator peer sends a DELETE message for that file, the space used by these chunks will never be reclaimed. Can you think of a change to the protocol, possibly including additional messages, that would allow to reclaim storage space even in that event?*

Since every initiator stores the peers that are storing its backed up chunks,
there is a possbility to verify what peers actually were able to delete these chunks,
using a reply message.
In our case, `1.1 DELETED <SenderID> <ChunkID>` serves as this verification.

Simultaneously, an updated version of peer would broadcast the chunks it currently has,
so that all initiators can check if any of these are supposed to have been deleted,
and promptly send a new `DELETE` message.