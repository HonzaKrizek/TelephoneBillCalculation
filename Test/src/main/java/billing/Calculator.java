package billing;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Calculator implements TelephoneBillCalculator{



    private ArrayList<Long> numbers;
    private ArrayList<Double> prices;
    private Map<Long,Integer> duplicates;

    // výpočet doby hovoru v sekundách
    private Integer getDurationSec(LocalDateTime start, LocalDateTime end){
        Integer startCallSec = start.getHour()*3600 + start.getMinute()*60 + start.getSecond();
        Integer endCallSec = end.getHour()*3600 + end.getMinute()*60 + end.getSecond();
        Integer durationSec = Math.max(startCallSec,endCallSec) - Math.min(startCallSec,endCallSec);
        if( ((start.getDayOfMonth() +1) == end.getDayOfMonth()) || ((start.getDayOfMonth() -1) == end.getDayOfMonth()) ) {
            durationSec = 24*3600 - durationSec;
        }
        return durationSec;
    }

    // výpočet ceny hovoru
    private Double getPrice(LocalDateTime start, LocalDateTime end){
        Integer durationSec = getDurationSec(start,end);
        Integer durationMin = durationSec/60;
        if((durationSec%60) !=0) {
            durationMin = durationSec/60 + 1;
        }

        Double price = 0d;
        Integer leftBoundary = 8;
        Integer rightBoundary = 16;
        // mimo dobu od 8:00 do 16:00
        if (start.getHour() >= rightBoundary || end.getHour() < leftBoundary){
            if (durationMin <= 5) {
                price = durationMin*0.5;
            } else {
                price = (durationMin - 5)*0.2 + 5*0.5;
            }
        }
        // v době od 8:00 do 16:00
        else if (start.getHour() >= leftBoundary && end.getHour() < rightBoundary) {
            if (durationMin <= 5) {
                price = durationMin*1d;
            } else {
                price = (durationMin - 5)*0.2 + 5*1;
            }
        }
        // v dobe přeš hranici od 8:00 do 16:00
        else {
            // začátek před 8:00
            if(start.getHour() < leftBoundary){
                Integer durationBeforeSec = 8*3600 - (start.getHour()*3600 + start.getMinute()*60 + start.getSecond());
                Integer durationBeforeMin = durationSec/60;
                if((durationBeforeSec%60) !=0) {
                    durationBeforeMin = durationBeforeSec/60 + 1;
                }
                Integer durationAfterSec = end.getHour()*3600 + end.getMinute()*60 + end.getSecond()
                        - (8*3600 + start.getSecond());
                Integer durationAfterMin = durationSec/60;
                if( ((durationAfterSec%60) !=0) && (!durationBeforeMin.equals(durationMin)) ) {
                    durationAfterMin = durationAfterSec/60 + 1;
                }
                if(durationMin <= 5){
                    price = durationBeforeMin*0.5 + durationAfterMin*1;
                } else {
                    if (durationBeforeMin < 5){
                        price = durationBeforeMin*0.5 + (5 - durationBeforeMin)*1 + (durationAfterMin - (5 - durationBeforeMin))*0.2;
                    }
                    else {
                        price = 5*0.5 +(durationBeforeMin-5)*0.2 + durationAfterMin*0.2;
                    }
                }
            } else {
                // začátek před 16:00
                Integer durationBeforeSec = 16*3600 - (start.getHour()*3600 + start.getMinute()*60 + start.getSecond());
                Integer durationBeforeMin = durationSec/60;
                if((durationBeforeSec%60) !=0) {
                    durationBeforeMin = durationBeforeSec/60 + 1;
                }
                Integer durationAfterSec = end.getHour()*3600 + end.getMinute()*60 + end.getSecond()
                        - (16*3600 + start.getSecond());
                Integer durationAfterMin = durationSec/60;
                if( ((durationAfterSec%60) !=0) && (!durationBeforeMin.equals(durationMin)) ) {
                    durationAfterMin = durationAfterSec/60 + 1;
                }
                if(durationMin <= 5){
                    price = durationBeforeMin*1 + durationAfterMin*0.5;
                } else {
                    if (durationBeforeMin < 5){
                        price = durationBeforeMin*1 + (5 - durationBeforeMin)*0.5 + (durationAfterMin - (5 - durationBeforeMin))*0.2;
                    }
                    else {
                        price = 5 +(durationBeforeMin-5)*0.2 + durationAfterMin*0.2;
                    }
                }
            }
        }
        return price;
    }


    @Override
    public BigDecimal calculate(String phoneLog) {
        try(BufferedReader br = Files.newBufferedReader(Paths.get(phoneLog), Charset.defaultCharset())) {

            numbers = new ArrayList<>();
            prices = new ArrayList<>();
            duplicates = new HashMap<>();
            String s;
            while ((s= br.readLine())!=null){
                String[] splits = s.split(",");
                LocalDateTime startCall = LocalDateTime.parse(splits[1], DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                LocalDateTime endCall = LocalDateTime.parse(splits[2], DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));

                Long phoneNumber = Long.parseLong(splits[0]);
                numbers.add(phoneNumber);
                // nalezení duplikací hovoru
                if (duplicates.containsKey(phoneNumber)) {
                    duplicates.replace(phoneNumber, duplicates.get(phoneNumber)+1);
                } else {
                    duplicates.put(phoneNumber,1);
                }

                Double price = getPrice(startCall,endCall);
                prices.add(price);
            }

            promo();
            // celková cena
            return BigDecimal.valueOf(prices.stream().mapToDouble(a->a).sum());

        }catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // nalezení nejčasteji volaného čísla
    private void promo(){
        int max = duplicates.
                values().
                stream().
                max(Integer::compare).
                get();
        long maxNumber = duplicates.
                entrySet().stream().
                filter(a->a.getValue()==max).
                mapToLong(Map.Entry::getKey).
                max().
                getAsLong();

        for ( Long number: numbers){
            if (Objects.equals(number, maxNumber) ){
                prices.remove(prices.get(numbers.indexOf(number)));
            }
        }
    }

}
