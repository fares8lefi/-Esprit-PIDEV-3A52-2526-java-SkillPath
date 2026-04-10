package test;

import Services.UserService;

import java.sql.SQLDataException;

public class Main {
    public static void main(String[] args) {


        UserService servicePersonne = new UserService();
        try {

            System.out.println(servicePersonne.recuperer());
        } catch (SQLDataException e) {
            throw new RuntimeException(e);
        }

    }
}
