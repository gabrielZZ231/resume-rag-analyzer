package org.acme.analysis.adapter.out;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AnalysisEventEmitter {

    private final BroadcastProcessor<String> processor = BroadcastProcessor.create();

    public void emit(String message) {
        processor.onNext(message);
    }

    public Multi<String> getUpdates() {
        return processor;
    }
}
