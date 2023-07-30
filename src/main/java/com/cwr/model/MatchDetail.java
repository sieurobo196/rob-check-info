package com.cwr.model;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class MatchDetail {
    String matchName;
    String matchType;
    String score;
    String msgAlert;
    Set<String> listMsgScoreChange;
}
