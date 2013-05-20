<%-- 
    Document   : index
    Created on : May 14, 2013, 12:04:32 PM
    Author     : MuradI
--%>


<%@page import="java.sql.ResultSet"%>
<%@page import="com.mysql.jdbc.Driver"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.sql.DriverManager"%>
<%@page import="java.io.PrintWriter"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.util.List"%>
<%@page import="az.its.pojo.Task"%>
<%
  //List<Task> tasks=(List<Task>)request.getSession().getAttribute("tasks");
  //SimpleDateFormat istediyimizFormat=new SimpleDateFormat(“MM dd yyyy”);
      String query="SELECT ID_,ACTORID_,NAME_,CREATE_,END_ from jbpm_taskinstance WHERE "+
" TASK_ in "+
"(select ID_ from jbpm_task where PROCESSDEFINITION_ in"+
"(SELECT PROCESSDEFINITION_ from jbpm_processinstance  p where ID_  in "+
"( SELECT  PROCESSINSTANCE_ from jbpm_variableinstance WHERE STRINGVALUE_=? )))"+
"and "+
"PROCINST_ in "+
"(SELECT ID_ from jbpm_processinstance  p where ID_  in "+
"( SELECT  PROCESSINSTANCE_ from jbpm_variableinstance WHERE STRINGVALUE_=? )) ORDER BY ID_"+
";";
  
 String query2="select * from  jbpm_node where ID_ in "+
"(select NODE_ from jbpm_token WHERE  END_ is not null and PROCESSINSTANCE_ in"+
"(select PROCESSINSTANCE_ from jbpm_variableinstance WHERE STRINGVALUE_=?));";
 
            List<Task> tasks=new ArrayList<Task>();
            Task lasttask=null;
            
            String uuid=request.getParameter("uuid");
           
            //connection info
            if(uuid!=null){
            DriverManager.registerDriver(new Driver());
            Connection connection=DriverManager.getConnection("jdbc:mysql://localhost:3306/okmdb", "openkm", "*secret*");
            PreparedStatement statement=connection.prepareStatement(query);
            statement.setString(1, uuid);
            statement.setString(2, uuid);
            ResultSet rs=statement.executeQuery();
            
            //list addd rows
            while(rs.next()){
               
                Task task = new Task();
                task.setId(rs.getInt(1));
                task.setActorId(rs.getString(2));
                task.setName(rs.getString(3));
                task.setStart(rs.getTimestamp(4));
                task.setEnd(rs.getTimestamp(5));
                tasks.add(task);
            }
            
            
            
            //list add last task row
            statement=connection.prepareStatement(query2);
            statement.setString(1, uuid);
            rs=statement.executeQuery();
            
            while(rs.next()){
               lasttask=new Task(1, "finish", rs.getString(3),tasks.get(tasks.size()-1).getEnd(),tasks.get(tasks.size()-1).getEnd());
               tasks.add(lasttask);
            }
           
            
            //connection close
            rs.close();
            statement.close();
            connection.close();
            }
String to=null;
%>
<%!
public String format2(Date d){
    SimpleDateFormat format=new SimpleDateFormat("yyyy-dd-MM  HH:mm:ss");

    if(d==null){ return "";}
    return format.format(d).toString();
}
public String checkTo(String s1,String s2){
   if(s2==null)
       return s1;
   
   return s2;
}
%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title></title>
        <style>
            .datagrid table { border-collapse: collapse; text-align: left; width: 100%; } 
            .datagrid {font: normal 12px/150% Arial, Helvetica, sans-serif; background: #fff; overflow: hidden; border: 1px solid #8C8C8C; -webkit-border-radius: 3px; -moz-border-radius: 3px; border-radius: 3px; }
            .datagrid table td, .datagrid table th { padding: 3px 10px; }
            .datagrid table thead th {background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #8C8C8C), color-stop(1, #7D7D7D) );background:-moz-linear-gradient( center top, #8C8C8C 5%, #7D7D7D 100% );filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#8C8C8C', endColorstr='#7D7D7D');background-color:#8C8C8C; color:#FFFFFF; font-size: 13px; font-weight: bold; border-left: 1px solid #A3A3A3; }
            .datagrid table thead th:first-child { border: none; }
            .datagrid table tbody td { color: #7D7D7D; border-left: 1px solid #DBDBDB;font-size: 12px;font-weight: normal; }.datagrid table tbody .alt td { background: #EBEBEB; color: #7D7D7D; }
            .datagrid table tbody td:first-child { border-left: none; }
            .datagrid table tbody tr:last-child td { border-bottom: none; }
            .datagrid table tfoot td div { border-top: 1px solid #8C8C8C;background: #EBEBEB;} 
            .datagrid table tfoot td { padding: 0; font-size: 12px } 
            .datagrid table tfoot td div{ padding: 2px; }
            .datagrid table tfoot td ul { margin: 0; padding:0; list-style: none; text-align: right; }
            .datagrid table tfoot  li { display: inline; }
            .datagrid table tfoot li a { text-decoration: none; display: inline-block;  padding: 2px 8px; margin: 1px;color: #F5F5F5;background:-webkit-gradient( linear, left top, left bottom, color-stop(0.05, #8C8C8C), color-stop(1, #7D7D7D) );background:-moz-linear-gradient( center top, #8C8C8C 5%, #7D7D7D 100% );filter:progid:DXImageTransform.Microsoft.gradient(startColorstr='#8C8C8C', endColorstr='#7D7D7D');background-color:#8C8C8C; }
            .datagrid table tfoot ul.active, 
            .datagrid table tfoot ul a:hover { text-decoration: none;border-color: #7D7D7D; color: #F5F5F5; background: none; background-color:#8C8C8C;}
        </style>
    </head>
    <body>
        <div class="datagrid">
       
        <%if(tasks!=null){%>
        <table border="1">
            <thead>
                <tr>
                    <th>FROM</th>
                    <th>Actor</th>
                    <th>NAME</th>
                    <th>START</th>
                    <th>END</th>
                </tr>
            </thead>
            <tbody>
                <%
                Task previousTask = null;
                for(Task t:tasks){
                    if (t.getName().equals("start"))
                        continue;
    %>
                <tr>
                    
                    <td><% if (previousTask != null) out.print(previousTask.getActorId()); else out.print("start"); %></td>
                    <td><%=t.getActorId() %></td>
                    <td><%=t.getName() %></td>
                    <td><%=format2(t.getStart()) %></td>
                    <td><%=format2(t.getEnd()) %></td>
                </tr>
                <%
                previousTask = t;
                }%>
        </table>

        <%}%>
        </div>
    </body>
</html>
