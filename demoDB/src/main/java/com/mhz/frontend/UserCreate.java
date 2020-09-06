package com.mhz.frontend;
import com.mhz.booking.Query;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.TransactionIsolationLevel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

public class UserCreate extends HttpServlet {
    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String userName = req.getParameter("userName");
        String password = req.getParameter("userPass");
        int deposit = Integer.parseInt(req.getParameter("deposit"));
        res.setContentType("text/html");
        PrintWriter pw = res.getWriter();
        Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
        SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE);
        Query tmpQuery = new Query(session);
        String createResult = tmpQuery.createUser(userName, password, deposit, 4);
        pw.println(createResult);
        req.getRequestDispatcher("index.html").include(req, res);
    }
}
