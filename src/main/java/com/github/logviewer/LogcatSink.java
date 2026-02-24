package com.github.logviewer;

import java.util.List;

/**
 * push the logcat data to the consumer
 * <p>
 * {@link #appendList} and {@link #onFinish} will be called within the same
 * CoroutineScope in {@link LogcatReader}
 */
public interface LogcatSink {
    /**
     * Append chunk of [LogItem] data.
     *
     * @param newItems a chunk of logcat entries
     */
    void appendList(List<LogItem> newItems);

    /**
     * If all logcat entries are send
     */
    default void onFinish() {}
}
