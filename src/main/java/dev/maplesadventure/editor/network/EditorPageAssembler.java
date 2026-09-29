package dev.maplesadventure.editor.network;

import java.io.ByteArrayOutputStream;
import java.util.*;

/** Bounded, in-order assembly. A partial snapshot is never exposed as authoritative state. */
public final class EditorPageAssembler {
    private static final int MAX_TRANSFERS=4;
    private record Transfer(UUID session,EditorPayloads.Kind kind,int count,List<byte[]> chunks){}
    private final Map<UUID,Transfer> transfers=new HashMap<>();

    /** Returns null until the complete transfer has arrived. Throws and clears on invalid ordering. */
    public byte[] accept(EditorPayloads.Page page){
        try{
            var transfer=transfers.get(page.transfer());
            if(transfer==null){
                if(transfers.size()>=MAX_TRANSFERS)throw new IllegalArgumentException("Too many editor transfers");
                transfer=new Transfer(page.session(),page.kind(),page.count(),new ArrayList<>());
                transfers.put(page.transfer(),transfer);
            }
            if(!transfer.session().equals(page.session())||transfer.kind()!=page.kind()
                    ||transfer.count()!=page.count()||page.index()!=transfer.chunks().size())
                throw new IllegalArgumentException("Out of order editor transfer");
            transfer.chunks().add(page.bytes());
            if(transfer.chunks().size()<transfer.count())return null;
            transfers.remove(page.transfer());
            var result=new ByteArrayOutputStream();
            for(var chunk:transfer.chunks())result.writeBytes(chunk);
            return result.toByteArray();
        }catch(RuntimeException failure){clear();throw failure;}
    }
    public void clear(){transfers.clear();}
    public int pendingTransfers(){return transfers.size();}
}
