package cz.aron.transfagent.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BulkOperation {
    
    
    @FunctionalInterface
    static public interface Operation<T> {
        void run(List<T> param);
    };


    static public <T> void run(Collection<T> coll, int batchSize,  Operation<T> oper ) {
        List<T> batch = new ArrayList<>(batchSize);
        for(T item: coll) {
            if(batch.size()==batchSize) {
                oper.run(batch);
                batch.clear();
            }
            batch.add(item);
        }
        if(batch.size()>0) {
            oper.run(batch);
        }
    }
}
