package Services;

import Models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements Iservice <User>{
    private Connection connection;
    public UserService() {
        connection = Utils.Database.getInstance().getConnection();
    }

    @Override
    public void ajouter(User user) throws SQLDataException {

    }

    @Override
    public void supprimer(User user) throws SQLDataException {

    }

    @Override
    public void modifier(User user) throws SQLDataException {

    }

    @Override
    public List<User> recuperer() throws SQLDataException {
        String sql = "SELECT * FROM users";
        List<User> personneList = new ArrayList<>();
        
        if (this.connection == null) {
            System.out.println("Impossible de récupérer les données : la connexion est nulle.");
            return personneList;
        }

        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            personneList = new ArrayList<>();
            while (rs.next()) {
                User p = new User();
                p.setUsername(rs.getString("username"));
                p.setEmail(rs.getString("email"));
                personneList.add(p);

            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return personneList;
    }
}
