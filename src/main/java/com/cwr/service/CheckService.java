package com.cwr.service;

import com.cwr.model.MatchData;
import com.cwr.model.MatchDetail;
import com.cwr.model.PostMessageDto;
import com.cwr.utils.HttpUtil;
import com.cwr.utils.StringJsonUtil;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;

@Component
@EnableScheduling
public class CheckService {
    private final static Logger logger = LoggerFactory.getLogger(CheckService.class);
    @Value("${slack.webhook.url-monitor}")
    private String urlChanelMonitor;
    @Value("${slack.webhook.channel}")
    private String channel;
    @Value("${slack.webhook.username}")
    private String username;
    @Value("${slack.webhook.token}")
    private String token;
    @Value("${slack.webhook.cookie}")
    private String cookie;
    @Value("${slack.webhook.phut}")
    private int phut;
    @Value("${slack.webhook.odd}")
    private double odd;
    private Map<String, MatchDetail> listMatchBetterOdd = new HashMap<>();
    private Map<String, String> scoreMatchList = new HashMap<>();

    @Scheduled(cron = "0 * * * * *")
    public void checkInfoService() throws UnirestException {
        logger.info("rob------------check here");
        String jsonData = getByUnirest();
        if(Objects.nonNull(jsonData)) {
            Gson gson = new Gson();
            MatchData matchData = gson.fromJson(jsonData, MatchData.class);
            matchData.getData().forEach(item -> {
                handleEachMatch(item);
            });
            handleAlertToChanelSlack("Check------------------------line");
        }

    }

    private String getByUnirest() {
        try {
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.get("https://prod20091.bti.bet/api/eventlist/eu/events/live")
                    .header("Content-Type", "application/json")
                    .header("Authorization", token)
                    .header("Cookie", cookie)
                    .asString();
            return response.getBody();
        } catch (Exception exception) {
            handleAlertToChanelSlack("GET data error------------------------line");
            return null;
        }
    }

    private void handleEachMatch(List<Object> match) {
        String typeName = (String) match.get(4);

        if (typeName.equals("Bóng đá")) {
            String matchType = (String) match.get(2);
            String matchName = (String) match.get(10);
            if (!matchType.startsWith("E-Soccer")) {

                // get Time Match
                Object matchTime = match.get(15);
                @SuppressWarnings("unchecked")
                Map<String, Object> matchTimeMap = (Map<String, Object>) matchTime;
                for (Map.Entry<String, Object> entry : matchTimeMap.entrySet()) {
                    String key = entry.getKey();
                    if (key.equals("GameTime")) {
                        Double value = (Double) entry.getValue();
                        value = value / 60;
                        Integer timeMatch = value.intValue();

                        if (timeMatch > phut) {

                            // ti so
                            List<String> tiso = (List<String>) match.get(12);
                            String scoreMatch = tiso.get(0) + "-" + tiso.get(1);

                            //get odds tai xiu
                            List<List<Object>> listOdds = (List<List<Object>>) match.get(19);
                            listOdds.forEach(oddObj -> {
                                String nameOdd = (String) oddObj.get(1);
                                if (nameOdd.contains("Cược trực tiếp Tài/Xỉu")) {
                                    List<List<Object>> oddTaiXiu = (List<List<Object>>) oddObj.get(7);
                                    oddTaiXiu.forEach(item -> {
                                        Object nameOddTaixiu = item.get(1);
                                        Map<String, Object> nameOddTaixiuObj = (Map<String, Object>) nameOddTaixiu;
                                        for (Map.Entry<String, Object> nameOddTaixiuObjSet : nameOddTaixiuObj.entrySet()) {
                                            String valueTaiOrXiu = (String) nameOddTaixiuObjSet.getValue();

                                            if (valueTaiOrXiu.startsWith("Tài")) {
                                                Double oddTai = (Double) item.get(4);
//                                                System.out.println("valueTaiOrXiu: " + valueTaiOrXiu);
//                                                System.out.println("oddTai: " + oddTai);
//                                                String msgToAlert = timeMatch + "' <-> score:  " + scoreMatch + "  <-> " + valueTaiOrXiu + ": " + oddTai;
//                                                MatchDetail matchDetail = new MatchDetail();
//                                                matchDetail.setMatchType(matchType);
//                                                matchDetail.setMatchName(matchName);
//                                                matchDetail.setScore(scoreMatch);
//                                                matchDetail.setMsgAlert(msgToAlert);
//                                                Set<String> stringSet = Objects.isNull(matchDetail.getListMsgScoreChange()) ? new HashSet<>() : matchDetail.getListMsgScoreChange();
//                                                stringSet.add(msgToAlert);
//                                                matchDetail.setListMsgScoreChange(stringSet);
//                                                listMatchBetterOdd.put(matchName, matchDetail);

                                                String msgAlert = timeMatch + "' <-> score:  " + scoreMatch + "  <-> " + valueTaiOrXiu + ": " + oddTai + "\n";
                                                if (!Objects.isNull(scoreMatchList.get(matchName)) && !scoreMatchList.get(matchName).equals(scoreMatch)) {
                                                    msgAlert = timeMatch + "' <-> score:  " + scoreMatchList.get(matchName) + "  <-> " + valueTaiOrXiu + ": " + oddTai + "\n" +
                                                            "----Goal--------" + scoreMatch;
                                                }
                                                String msg = "";
                                                if (oddTai > 0 && oddTai < odd) {
                                                    System.out.println("oddTai: " + matchName + "--" + scoreMatchList.get(matchName));

                                                    msg = "--Time: " + getDate() + "\n" +
                                                            "--" + matchType +
                                                            "-" + matchName + "\n" +
                                                            msgAlert +
                                                            "---------------------------";

//
                                                }
                                                handleAlertToChanelSlack(msg);
                                                scoreMatchList.put(matchName, scoreMatch);


                                            }
                                        }
                                    });
                                }
                            });
                        }

                    }

                }

            }

        }
    }

    private void callHandleSendAlertForUser() {
        for (Map.Entry<String, MatchDetail> entry : listMatchBetterOdd.entrySet()) {
            String matchName = entry.getKey();
            MatchDetail matchDetail = entry.getValue();
            System.out.println("rob---[" + matchName + "]");
//            if
            matchDetail.getListMsgScoreChange().forEach(item -> {
                System.out.println("rob---****" + matchDetail.getMsgAlert() + "****");
            });
            // Sử dụng key và value ở đây
        }
    }

    private void handleAlertToChanelSlack(String msg) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-type", "application/json");
            PostMessageDto postMessageDto = PostMessageDto.builder()
                    .channel(channel)
                    .username(username)
                    .text(msg)
                    .build();
            String json = StringJsonUtil.toStringJson(postMessageDto);
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.post("https://hooks.slack.com/services/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")
                    .header("Content-Type", "application/json")
                    .body(json)
                    .asString();
            if (response.getStatus() != 200) {
                System.out.println("Can not send message to Slack channel. Error: " + response.getBody());
            }
        } catch (Exception exception) {
            System.out.println("Can not send message to Slack channel. Error: " + exception.getMessage());
        }

    }

    private String getDate() {
        Date date = new Date();

        // Create a SimpleDateFormat object with the desired format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        // Format the date using the SimpleDateFormat object
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
}
