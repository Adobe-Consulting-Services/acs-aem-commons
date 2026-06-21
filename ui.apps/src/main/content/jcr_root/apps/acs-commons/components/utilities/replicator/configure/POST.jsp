<%@include file="/libs/foundation/global.jsp" %><%


    String action = slingRequest.getParameter(":action");
    RequestDispatcher requestDispatcher = slingRequest.getRequestDispatcher(action);

    requestDispatcher.forward(slingRequest, slingResponse);
%>