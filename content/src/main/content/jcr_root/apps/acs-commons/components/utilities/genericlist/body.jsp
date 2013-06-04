<%@include file="/libs/foundation/global.jsp"%>

<body>

    <h1>Generic List - <%= currentPage.getTitle() %></h1>

    <h2>List Items:</h2>

    <ul>
        <cq:include path="list" resourceType="foundation/components/parsys"/>
    </ul>

</body>