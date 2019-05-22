package com.wojtasik;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Main {

    private static Scanner sc = new Scanner(System.in);
    private static Datasource datasource = new Datasource();

    public static void main(String[] args) {

        if (!datasource.open()) {
            System.out.println("Can't open datasource");
            return;
        }





//
//        boolean mainQuit = false;
//        int choose;
//        while (!mainQuit) {
//
//            printMenu();
////            System.out.println("");
//            System.out.print("Please enter your choose...");
//            choose = inputValidation();
//            //   clrscrn();
//            switch (choose) {
//                case 1:
//                    login();
//                    break;
//                case 2:
//                    votingRules();
//                    sc.nextLine();
//                    break;
//                case 3:
//                    break;
//                case 4:
//                    printMenu();
//                    break;
//                case 0:
//                    mainQuit = true;
//                    System.out.println("Quitting program...");
//                    break;
//            }
//        }
//        datasource.close();
//    }
//
//    private static void printMenu() {
//        System.out.println("==== VOTING MANAGER ====");
//        System.out.println("1.  Login to vote");
//        System.out.println("2.  Voting rules");
//        System.out.println("3.  Voting report");
//        System.out.println("4.  Print Menu");  // The same value is thrown while input in Scanner is wrong (can't parse to int value). See inputValidation()
//        System.out.println("0.  QUIT program");
//    }
//
//    private static void login() {
//        boolean logout = false;
//        System.out.println("==== LOGIN TO VOTE ====");
//        System.out.println("* You can always logout by typing \"L\".");
//        System.out.println();
//        System.out.println("Please enter your first name");
//        String name = sc.next();
//        System.out.println(name);
//        if (!name.equals("L")) {
//            System.out.println("Please enter your pesel number");
//            String pesel = sc.next().replaceAll("\\s", "");
//            System.out.println(pesel);
//            if (!pesel.equals("L")) {
//                if (datasource.votingAccess(name, pesel)) {
//                    datasource.printCandidates();
//                    System.out.println("Choose your type: ");
//                    String choose;
//                    choose = sc.next();
//                    choose += sc.nextLine();
//                    datasource.vote(name, pesel, choose);
//                    System.out.println("Thank you for your vote...");
//                    System.out.println();
//                    System.out.println("Voting results loading");
//                    keepCalm();
//                    datasource.printVoteResults();
//
//                }
//            }
//        }
//    }
//
//    private static void votingRules() {
//        System.out.println("==== VOTING RULES ====");
//        System.out.println("* You must be over 18 to vote.");
//        System.out.println("* While logging in please prepare your PESEL number.");
//        System.out.println("* You are allowed to vote ONLY for one candidate. Multiple vote cards will be invalid.");
//        System.out.println("* List of candidates and their parties will appear while voting.");
//        System.out.println("* Your vote for specific candidate means automatically vote for his party.");
//        System.out.println();
//        System.out.println("Press any key to continue...");
//    }
//
//    private static int inputValidation() {
//        int chooseInt;
//        String chooseString = sc.next();
//        try {
//            chooseInt = Integer.parseInt(chooseString);
//            if (chooseInt >= 0 && chooseInt <= 4) {
//                return chooseInt;
//            } else {
//                System.out.println("Your choose must be integer, between 0 and 4.");
//                return 4;
//            }
//        } catch (NumberFormatException e) {
//            System.out.println("You need to choose NUMBER from a list");
//            return 4;
//        }
//    }
//
//    public static void keepCalm() {
//        try {
//            for (int i = 0; i <= 2; i++) {
//                System.out.print(" . ");
//                TimeUnit.SECONDS.sleep(1);
//            }
//        } catch (InterruptedException e) {
//            System.out.println("An unexpected error: " + e.getMessage());
//        }
    }
//

}
