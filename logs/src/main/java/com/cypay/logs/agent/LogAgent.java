package com.cypay.logs.agent;

import com.cypay.logs.model.LogEntry;

import java.util.List;

public interface LogAgent {
    List<LogEntry> getLogs(String acteur);
}
