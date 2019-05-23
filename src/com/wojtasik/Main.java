package com.wojtasik;


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

        boolean mainQuit = false;
        int choose;
        while (!mainQuit) {

            printMenu();
//            System.out.println("");
            System.out.print("Please enter your choose...");
            choose = inputValidation();
            //   clrscrn();
            switch (choose) {
                case 1:
                    login();
                    sc.next();
                    break;
                case 2:
                    votingRules();
                    sc.next();
                    break;
                case 3:
                    datasource.printVoteResults("Y","Y");
                    sc.next();
                    break;
                case 4:
                    break;
                case 0:
                    mainQuit = true;
                    System.out.println("Quitting program...");
                    break;
            }
        }
        datasource.close();
    }

    private static void printMenu() {
        System.out.println("==== VOTING MANAGER ====");
        System.out.println("1.  Login to vote");
        System.out.println("2.  Voting rules");
        System.out.println("3.  Voting report");
        System.out.println("4.  Print Menu");  // The same value is thrown while input in Scanner is wrong (can't parse to int value). See inputValidation()
        System.out.println("0.  QUIT program");
    }

    private static void login() {
        System.out.println("==== LOGIN TO VOTE ====");
        System.out.println("* You can always logout by typing \"L\".");
        System.out.println();
        System.out.println("Please enter your first name");
        String firstName = sc.next();
        if (!firstName.equals("L")) {
            System.out.println("Please enter your last name");
            String lastname = sc.next();
            if(!lastname.equals("L")) {
                String pesel;
                System.out.println("Please enter your pesel number");
                pesel=sc.next();                        //This "strange" pesel read allow user to use spaces before, between or after
                pesel+=sc.nextLine();                   //digits and pesel will be transformed correctly anyway (it's important input
                pesel = pesel.replaceAll("\\s", "");    // that's why so many attention put here
                if (!pesel.equals("L")) {
                    String name = firstName+" "+lastname;
                    if (datasource.votingAccess(name, pesel)) {
                        datasource.printCandidates();
                        System.out.println("Choose your type: ");
                        String choose;
                        choose = sc.next();
                        choose += sc.nextLine();
                        datasource.vote(name, pesel, choose);
                        System.out.println();
                        System.out.println("Voting results loading");
                        keepCalm();
                        String graph;
                        System.out.println("Do you want to print primitive bar graph? (Y/N)");
                        graph = sc.next();
                        graph = String.valueOf(graph.charAt(0)).toUpperCase();
                        datasource.printVoteResults(graph, "N");

                        System.out.println("Press 'C' key to continue...");
                    }
                    else{
                        System.out.println("Press 'C' key to continue...");
                    }
                }
            }
        }
    }

    private static void votingRules() {
        System.out.println("");
        System.out.println("======= VOTING RULES =======");
        System.out.println();
        System.out.println("* You must be over 18 to vote.");
        System.out.println("* While logging in please prepare your PESEL number.");
        System.out.println("* You are allowed to vote ONLY for one candidate. Multiple vote cards will be invalid.");
        System.out.println("* List of candidates and their parties will appear while voting.");
        System.out.println("* Your vote for specific candidate means automatically vote for his party.");
        System.out.println();
        System.out.println("Press 'C' key to continue...");
    }

    private static int inputValidation() {
        int chooseInt;
        String chooseString = sc.next();
        try {
            chooseInt = Integer.parseInt(chooseString);
            if (chooseInt >= 0 && chooseInt <= 4) {
                return chooseInt;
            } else {
                System.out.println("Your choose must be integer, between 0 and 4.");
                return 4;
            }
        } catch (NumberFormatException e) {
            System.out.println("You need to choose NUMBER from a list");
            return 4;
        }
    }

    private static void keepCalm() {
        try {
            for (int i = 0; i <= 1; i++) {
                System.out.print(" . ");
                TimeUnit.SECONDS.sleep(1);
            }
            System.out.println();
        } catch (InterruptedException e) {
            System.out.println("An unexpected error: " + e.getMessage());
        }
    }
}
