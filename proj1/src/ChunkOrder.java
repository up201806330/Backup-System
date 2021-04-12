import java.util.Comparator;

// First order by (perceived - desired) replication degree, then by size
// Chunks with perceived degree larger than needed will be first, draws are resolved by chunk size,
public class ChunkOrder implements Comparator<Chunk> {
    @Override
    public int compare(Chunk left, Chunk right) {
        Integer leftRep = left.getPerceivedReplicationDegree() - left.getDesiredReplicationDegree();
        Integer rightRep = right.getPerceivedReplicationDegree() - right.getDesiredReplicationDegree();
        Integer leftSize = left.getContent().length;
        Integer rightSize = right.getContent().length;

        int byRepDegree = leftRep.compareTo(rightRep);
        if (byRepDegree != 0) return byRepDegree;
        else return leftSize.compareTo(rightSize);
    }
}
