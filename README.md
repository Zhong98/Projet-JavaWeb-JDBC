# Projet-JavaWeb-JDBC
1.After the login is successful, it will jump to pay.html, where you can request PayServlet to submit an order, and then complete the processing of the order request: reduce commodity inventory and reduce user balance operations(stock and balance can't less than 0).

2.Users must log in before they can place an order. (The login time limit is 10 minutes, that is, if there is no operation for 10 minutes after login, the page prompts you to log in when placing an order again.), only users in the user information table can log in successfully.

3.Create a table to save order information,it is necessary to ensure that the user's daily cumulative consumption cannot exceed the corresponding daily limit.
