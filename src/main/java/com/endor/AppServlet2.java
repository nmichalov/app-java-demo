package com.endor;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URL;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

@WebServlet(name = "AppServlet", urlPatterns = "/AppServlet")
public class AppServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        // Print HTML Form
        out.println("<html><head><title>SSRF Test</title></head><body>");
        out.println("<h1>SSRF Testing Application</h1>");
        out.println("<form action=\"/AppServlet\" method=\"get\">" +
                "URL: <input type=\"text\" name=\"ssrf\" id=\"ssrf\"> -- (If ssrf=file, inputs will be parsed from the file /opt/ssrfinput.txt)<br><br>" +
                "Https URL: <input type=\"text\" name=\"httpsssrf\" id=\"httpsssrf\"><br><br>" +
                "<input type=\"submit\" value=\"Submit\">" +
                "</form>");

        // Get parameters from the form
        String ssrfUrl = request.getParameter("ssrf");
        String httpsSsrfUrl = request.getParameter("httpsssrf");

        if (ssrfUrl != null && ssrfUrl.equalsIgnoreCase("file")) {
            processFile(request, response);
        } else if (ssrfUrl != null && !ssrfUrl.isEmpty()) {
            useUrlOpenConnection(request, response, ssrfUrl);
        } else if (httpsSsrfUrl != null && httpsSsrfUrl.toLowerCase().startsWith("https://")) {
            useUrlOpenConnectionHttps(request, response, httpsSsrfUrl);
        }

        out.println("</body></html>");
    }

    private void processFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        File file = new File("/opt/ssrfinput.txt");

        if (!file.exists()) {
            out.println("<p>Error: File '/opt/ssrfinput.txt' not found!</p>");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.println("<p>Processing URL: " + line + "</p>");
                useUrlOpenConnection(request, response, line);
                Thread.sleep(2000); // Simulate delay
            }
        } catch (Exception e) {
            out.println("<p>Error while reading the file: " + e.getMessage() + "</p>");
        }
    }

    private void useUrlOpenConnection(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<h3>Using URL.openConnection for: " + url + "</h3>");

        try {
            URL targetUrl = new URL(url);
            try (BufferedReader in = new BufferedReader(new InputStreamReader(targetUrl.openStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    out.println("<p>" + inputLine + "</p>");
                }
            }
        } catch (Exception e) {
            out.println("<p>Error during URL.openConnection: " + e.getMessage() + "</p>");
        }
    }

    private void useUrlOpenConnectionHttps(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        PrintWriter out = response.getWriter();
        out.println("<h3>Using HTTPS Connection for: " + url + "</h3>");

        String hostname = url.replaceFirst("https://", "");

        try {
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(hostname, 443)) {
                socket.startHandshake();
                PrintWriter socketOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
                socketOut.println("GET / HTTP/1.0");
                socketOut.println();
                socketOut.flush();

                if (socketOut.checkError()) {
                    out.println("<p>Error during HTTPS socket communication.</p>");
                }

                try (BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String inputLine;
                    while ((inputLine = socketIn.readLine()) != null) {
                        out.println("<p>" + inputLine + "</p>");
                    }
                }
            }
        } catch (Exception e) {
            out.println("<p>Error during HTTPS Connection: " + e.getMessage() + "</p>");
        }
    }
}
