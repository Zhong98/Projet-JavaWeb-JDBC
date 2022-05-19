package cn.yiliedu.servlet;

import cn.yiliedu.util.JDBCUtils;

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
import java.util.ArrayList;
import java.util.Date;

@WebServlet("/payServlet")
public class PayServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter writer = response.getWriter();

        String goodId = request.getParameter("goodId");
        String count = request.getParameter("count");
        Cookie[] cookies = request.getCookies();
        String customerId = null;
        int balance = 0;
        int stock = 0;
        int price = 0;
        int limit = 0;

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
                int id = Integer.parseInt(goodId);
                int num = Integer.parseInt(count);

                Connection connection = null;
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
                PreparedStatement preparedStatement7 = null;
                try {
                    connection.setAutoCommit(false);

                    //get customer's balance
                    String sql1 = "select balance from customers where id=?";
                    preparedStatement1 = connection.prepareStatement(sql1);
                    if (customerId != null) {
                        int clientId = Integer.parseInt(customerId);
                        System.out.println("-----------------------------");
                        System.out.println(clientId);
                        preparedStatement1.setInt(1, clientId);
                        ResultSet resultSet1 = preparedStatement1.executeQuery();
                        if (resultSet1.next()) {
                            balance = resultSet1.getInt(1);
                        }


                        //get product's stock
                        String sql2 = "select stock,price from products where id=?";
                        preparedStatement2 = connection.prepareStatement(sql2);
                        preparedStatement2.setInt(1, id);
                        ResultSet resultSet = preparedStatement2.executeQuery();
                        if (resultSet.next()) {
                            stock = resultSet.getInt(1);
                            price = resultSet.getInt(2);
                        }

                        boolean balanceEnough = true;

                        //get customer's limit
                        String sql6 = "select `limit` from customers where id=?";
                        preparedStatement6 = connection.prepareStatement(sql6);
                        preparedStatement6.setInt(1, clientId);
                        ResultSet resultSet2 = preparedStatement6.executeQuery();
                        if (resultSet2.next()) {
                            limit = resultSet2.getInt(1);
                        }

                        //Get the user's transaction record for the day, and get the remaining consumption limit today
                        int moneySpent = 0;
                        ArrayList<Integer> moneyList = new ArrayList<>(); //save each transaction's totalPrice
                        ArrayList<String> saleDate = new ArrayList<>(); //save date after format

                        String sql7 = "select `time`,totalPrice from sale_log where customerId=?";
                        preparedStatement7 = connection.prepareStatement(sql7);
                        preparedStatement7.setInt(1, clientId);
                        ResultSet resultSet3 = preparedStatement7.executeQuery();
                        while (resultSet3.next()) {
                            Timestamp timestampPrev = resultSet3.getTimestamp(1);
                            String timePrev = simpleDateFormat.format(timestampPrev);
                            int money = resultSet3.getInt(2);
                            saleDate.add(timePrev);
                            moneyList.add(money);
                        }
                        for (int i = 0; i < saleDate.size(); i++) {
                            if (saleDate.get(i).equals(today)) {
                                moneySpent += moneyList.get(i);
                            }
                        }

                        //Determine whether the remaining quota for the day is sufficient
                        if ((moneySpent+num*price)>limit){
                            balanceEnough=false;
                        }

                        //if balance and stock are enough and didn't pass daily limit.
                        if ((balance - num * price) > 0 && (stock - num) > 0 && balanceEnough) {
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
                            String sql5 = "insert into sale_log(customerId,productId,count,time,totalPrice) values(?,?,?,?,?)";
                            preparedStatement5 = connection.prepareStatement(sql5);
                            preparedStatement5.setInt(1, clientId);
                            preparedStatement5.setInt(2, id);
                            preparedStatement5.setInt(3, num);
                            preparedStatement5.setTimestamp(4, timestamp);
                            preparedStatement5.setInt(5, num * price);
                            preparedStatement5.executeUpdate();

                            writer.write("Thanks for your purchasing!");
                            connection.commit();
                        }
                    } else {
                        writer.write("The current login has expired, please log in again");
                        Thread.sleep(3000);
                        response.sendRedirect("http://localhost:8080/paydemo/web/index.html");
                    }
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                    try {
                        connection.rollback();
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                } finally {
                    try {
                        preparedStatement7.close();
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
            }
        }
    }
}
