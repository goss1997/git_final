<%--
  Created by IntelliJ IDEA.
  User: mac
  Date: 8/27/24
  Time: 9:28 AM
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Q&A 작성</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="icon" type="image/png" href="/images/FiguriumHand.png"/>
    <link rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"/>

    <script type="text/javascript">
        function updateTitle() {
            var category = document.getElementById('category').value;
            var titleField = document.getElementById('title');
            var titleValue = titleField.value;

            if (category && !titleValue.startsWith(`[${category}]`)) {
                titleField.value = `[${category}] ${titleValue}`;
            }
        }

        function goBack(){

            const refererUrl = document.getElementById('refererUrl').value;
            if (refererUrl) {
                window.location.href = refererUrl;
            } else {
                //referer가 없을때 사용
                window.history.back();
            }

        }
    </script>

    <style>
        /* 반응형을 위한 미디어 쿼리 */
        @media (min-width: 768px) {
            .qainsert_title {
                font-size: 30px;
            }
            .form-group {
                font-size: 16px;
            }
        }

        @media (max-width: 768px) {
            .qainsert_title {
                font-size: 28px;
            }
            .form-group {
                font-size: 14px;
            }

        }

        @media (max-width: 576px) {
            .qainsert_title {
                font-size: 24px;
            }

            .form-group {
                font-size: 12px;
            }
        }
    </style>
</head>
<jsp:include page="../common/header.jsp"/>
<div style="height: 90px"></div>
<body>

<div id="content-wrap-area">
<div class="container pt-3">
    <h1 class="qainsert_title" style="margin-bottom: 15px">Q&A 작성</h1>
    <hr>
    <form action="${pageContext.request.contextPath}/qa/qaSave.do" method="post">

        <div class="form-group">
            <input type="text" class="form-control" name="title" placeholder="제목을 입력하세요" autocomplete="off">
        </div>
        <div class="form-group">

            <div class="form-group">
                <select id="category" name="category" class="custom-select" onchange="updateTitle()">
                    <option selected disabled>:::분류 선택:::</option>
                    <option value="배송문의">배송문의</option>
                    <option value="환불문의">환불문의</option>
                    <option value="반품문의">반품문의</option>
                    <option value="배송지변경문의">배송지변경문의</option>
                    <option value="기타문의">기타문의</option>
                    <option value="상품문의">상품문의</option>
                    <option value="입고문의">입고문의</option>
                </select>
            </div>
            <textarea class="form-control" style="resize: none; height: 300px;" id="content" name="content"
                      placeholder="내용을 입력하세요" autocomplete="off"></textarea>
        </div>
        <input type="hidden" name="productId" value="${productId}">
        <input type="hidden" name="orderId" value="${requestScope.orderId}">
        <input type="hidden" id="refererUrl" value="${referer}">
        <a onclick="goBack()" class="btn btn-light" role="button" style="margin-bottom: 10px; float: right;">취소</a>
        <button type="submit" class="btn btn-dark" style="margin-bottom: 10px; float: right; margin-right: 10px;">등록</button>

    </form>
</div>
</div>

<!-- NOTE : 푸터바 -->
<jsp:include page="../common/footer.jsp"/>

</body>


</html>
