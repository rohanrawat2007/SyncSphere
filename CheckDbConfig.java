import com.syncsphere.database.DBConnection;
public class CheckDbConfig {
  public static void main(String[] args) {
    System.out.println(DBConnection.buildJdbcUrl());
  }
}
