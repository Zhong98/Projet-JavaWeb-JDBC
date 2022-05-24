package servlet;

import util.JDBCUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@WebServlet("/payServlet")
public class PayServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        this.doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter writer = response.getWriter();

        String goodId = request.getParameter("goodId");
        String count = request.getParameter("count");
        Cookie[] cookies = request.getCookies();
        String customerId = null;
        int balance;
        int stock;
        int price;
        int limit;

        for (int i = 0; i < cookies.length; i++) {
            if (cookies[i].getName().equals("customerId")) {
                customerId = cookies[i].getValue();
            }
        }

        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Timestamp timestamp = new Timestamp(date.getTime());
        String today = simpleDateFormat.format(date);

        if (goodId != null && count != null) {
            if (!goodId.isEmpty() && !count.isEmpty()) {
                if (customerId != null) {
                    int id = Integer.parseInt(goodId);
                    int num = Integer.parseInt(count);

                    Connection connection;
                    try {
                        connection = JDBCUtils.getConnection();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                    PreparedStatement preparedStatement1 = null;
                    PreparedStatement preparedStatement2 = null;
                    PreparedStatement preparedStatement3 = null;
                    PreparedStatement preparedStatement4 = null;
                    PreparedStatement preparedStatement5 = null;
                    PreparedStatement preparedStatement6 = null;
                    try {
                        connection.setAutoCommit(false);

                        //get customer's balance and limit
                        String sql1 = "select balance,`limit` from customers where id=?";
                        preparedStatement1 = connection.prepareStatement(sql1);

                        int clientId = Integer.parseInt(customerId);
                        preparedStatement1.setInt(1, clientId);
                        ResultSet resultSet1 = preparedStatement1.executeQuery();
                        if (resultSet1.next()) { //Make sure the system can get customer's info
                            balance = resultSet1.getInt(1);
                            limit = resultSet1.getInt(2);

                            //get product's stock
                            String sql2 = "select stock,price from products where id=?";
                            preparedStatement2 = connection.prepareStatement(sql2);
                            preparedStatement2.setInt(1, id);
                            ResultSet resultSet = preparedStatement2.executeQuery();
                            if (resultSet.next()) { //Make sure the system can get product's info
                                stock = resultSet.getInt(1);
                                price = resultSet.getInt(2);
                                boolean limitEnough = true;


                                //Get the user's transaction record for the day, and get the remaining consumption limit today
                                String sql6 = "select SUM(totalPrice) from sale_log where customerId=? and `date`=?";
                                preparedStatement6 = connection.prepareStatement(sql6);
                                preparedStatement6.setInt(1, clientId);
                                preparedStatement6.setString(2, today);
                                ResultSet resultSet3 = preparedStatement6.executeQuery();
                                int moneySpent += resultSet3.getInt(1);
                                


                                //Determine whether the remaining quota for today is enough
                                if ((moneySpent + num * price) > limit) {
                                    limitEnough = false;
                                }

                                //if balance and stock are enough and didn't pass daily limit.
                                if ((balance - num * price) > 0) {
                                    if ((stock - num) > 0) {
                                        if (limitEnough) {
                                            String sql3 = "update products set stock=? where id=?";
                                            String sql4 = "update customers set balance=? where id=?";
                                            balance = balance - num * price;
                                            stock = stock - num;
                                            preparedStatement3 = connection.prepareStatement(sql3);
                                            preparedStatement4 = connection.prepareStatement(sql4);
                                            preparedStatement3.setInt(1, stock);
                                            preparedStatement3.setInt(2, id);

                                            preparedStatement4.setInt(1, balance);
                                            preparedStatement4.setInt(2, clientId);

                                            preparedStatement3.executeUpdate();
                                            preparedStatement4.executeUpdate();

                                            //record order information
                                            String sql5 = "insert into sale_log(customerId,productId,count,time,totalPrice,`date`) values(?,?,?,?,?,?)";
                                            preparedStatement5 = connection.prepareStatement(sql5);
                                            preparedStatement5.setInt(1, clientId);
                                            preparedStatement5.setInt(2, id);
                                            preparedStatement5.setInt(3, num);
                                            preparedStatement5.setTimestamp(4, timestamp);
                                            preparedStatement5.setInt(5, num * price);
                                            preparedStatement5.setString(6, today);
                                            preparedStatement5.executeUpdate();

                                            writer.write("Thanks for your purchasing!");
                                            connection.commit();
                                        } else {
                                            writer.write("Sorry，your remaining limit for today isn't enough...");
                                        }
                                    } else {
                                        writer.write("Sorry,The current stock can't meet your request...");
                                    }
                                } else {
                                    writer.write("Sorry，your balance isn't enough...");
                                }
                            } else {
                                writer.write("Can't get the product's information...");
                            }
                        } else {
                            writer.write("Can't get the customer's information...");
                        }
                    } catch (SQLException e) {
                        System.out.println(e.getMessage());
                        try {
                            connection.rollback();
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    } finally {
                        try {
                            preparedStatement6.close();
                            preparedStatement5.close();
                            preparedStatement4.close();
                            preparedStatement3.close();
                            preparedStatement2.close();
                            preparedStatement1.close();
                            connection.close();
                        } catch (SQLException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                } else {
                    writer.write("The current login has expired, please log in again.");
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    response.sendRedirect("http://localhost:8080/paydemo/web/index.html");
                }
            }
        }
    }
}
