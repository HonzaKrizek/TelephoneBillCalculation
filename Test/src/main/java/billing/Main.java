package billing;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    public static void main(String[] args) {

        String file = "phoneLog.csv";

        TelephoneBillCalculator calculator = new Calculator();
        System.out.println(calculator.calculate("phoneLog1.csv") + "Kč");
        System.out.println(calculator.calculate("phoneLog2.csv") + "Kč");
        System.out.println(calculator.calculate("phoneLog3.csv") + "Kč");


    }
}
