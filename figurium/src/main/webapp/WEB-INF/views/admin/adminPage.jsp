<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>관리자 페이지</title>
    <link rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200"/>

    <link rel="icon" type="image/png" href="/images/FiguriumHand.png"/>
    <style>
        .thead-light > tr > th {
            text-align: center;
            vertical-align: middle !important;
        }

        tbody > tr > td {
            text-align: center;
            vertical-align: middle !important;
        }
        .nav-link:hover{
            cursor: pointer;
        }
    </style>
</head>

<body>
<!-- 메뉴바 -->
<jsp:include page="../common/header.jsp"/>
<div style="height: 90px"></div>
<div id="content-wrap-area">

    <nav class="navbar navbar-expand-sm bg-dark navbar-dark justify-content-center">
        <ul class="navbar-nav">
            <li class="nav-item">
                <a class="nav-link" style="font-size: 16px; vertical-align: middle !important;"
                   href="productInsertForm.do">상품 등록</a>
            </li>
            &nbsp;&nbsp;
            <li class="nav-item">
                <a class="nav-link" href="admin.do">주문조회</a>
            </li>
            &nbsp;&nbsp;

            <li class="nav-item">
                <a class="nav-link" id="changeStatus" href="adminRefund.do">배송상태 변경</a>
            </li>
            <li class="nav-item">
                <div class="icon-header-item cl2 hov-cl1 trans-04 p-r-11 p-l-10 icon-header-noti"
                     id="quantity-notify"
                     data-notify="0">
                    <a class="nav-link" style="font-size: 16px; vertical-align: middle !important; margin-top: 3px;"
                       href="adminQuantity.do">상품 재고수정</a>
                </div>
            </li>
            &nbsp;&nbsp;
            <li class="nav-item">
                <div class="icon-header-item cl2 hov-cl1 trans-04 p-r-11 p-l-10 icon-header-noti"
                     id="payment-notify"
                     data-notify="0">
                    <a class="nav-link" style="font-size: 16px; vertical-align: middle !important; margin-top: 3px;"
                       href="adminPayment.do">결제취소</a>
                </div>
            </li>
            &nbsp;&nbsp;
            <li class="nav-item">
                <div class="icon-header-item cl2 hov-cl1 trans-04 p-r-11 p-l-10 icon-header-noti"
                     id="retrun-notify"
                     data-notify="0">
                    <a class="nav-link" style="font-size: 16px; vertical-align: middle !important; margin-top: 3px;"
                       href="adminReturns.do">반품승인</a>
                </div>
            </li>
            &nbsp;&nbsp;
            <li class="nav-item">
                <div class="icon-header-item cl2 hov-cl1 trans-04 p-r-11 p-l-10 icon-header-noti"
                     id="qa-notify"
                     data-notify="0">
                    <a class="nav-link" style="font-size: 16px; vertical-align: middle !important; margin-top: 3px;"
                       id="viewQaList" href="adminQaList.do" >Q&A 미답변</a>
                </div>
            </li>
        </ul>
    </nav>

    <br><br>
    <table class="table table-hover" style="width: 90%; margin: auto">
        <thead class="thead-light">
        <tr>
            <th class="col-1">주문번호<br>주문일자</th>
            <th class="col-3">상품명</th>
            <th class="col-1">결제방식</th>
            <th class="col-1">총 결제금액</th>
            <th class="col-1">결제상태</th>
            <th class="col-1">주문상태</th>
        </tr>
        </thead>
        <tbody>
        <c:forEach var="order" items="${orderList}">
        <tr>
            <td>${order.id}<br>${order.createdAt}</td>
            <td>${order.productName}</td>
            <td>${order.paymentType == 'vbank' ? '무통장입금' : '카드결제'}</td>
            <td>${order.price}원</td>
            <td>${order.valid == 'y' ? '결제완료' : '환불완료'}</td>
            <td>${order.status}</td>
            <input type="hidden" name="ordersId" value="${order.id}">
        </tr>
        </c:forEach>
        </tbody>
    </table>

</div>

<!-- 푸터 -->
<jsp:include page="../common/footer.jsp"/>
</body>


<script>
    function updateCount() {
        $.ajax({
            url: 'count.do', // 컨트롤러에서 갯수를 가져오는 URL
            type: 'GET',
            dataType: 'json',
            success: function (response) {
                if (response.quantityCount !== undefined) {
                    $('#quantity-notify').attr('data-notify', response.quantityCount);
                } else {
                    $('#quantity-notify').attr('data-notify', '0'); // 갯수가 없을 경우 0으로 설정
                }
                if (response.paymentCount !== undefined) {
                    $('#payment-notify').attr('data-notify', response.paymentCount);
                } else {
                    $('#payment-notify').attr('data-notify', '0'); // 갯수가 없을 경우 0으로 설정
                }
                if (response.retrunCount !== undefined) {
                    $('#retrun-notify').attr('data-notify', response.retrunCount);
                } else {
                    $('#retrun-notify').attr('data-notify', '0'); // 갯수가 없을 경우 0으로 설정
                }
                if (response.qaCount !== undefined) {
                    $('#qa-notify').attr('data-notify', response.qaCount);
                } else {
                    $('#qa-notify').attr('data-notify', '0'); // 갯수가 없을 경우 0으로 설정
                }

            },
            error: function (xhr, status, error) {
                console.error('count 가져오는 데 실패했습니다.', error);
                $('#quantity-notify').attr('data-notify', '0'); // 오류 발생 시 0으로 설정
                $('#payment-notify').attr('data-notify', '0'); // 오류 발생 시 0으로 설정
                $('#retrun-notify').attr('data-notify', '0'); // 오류 발생 시 0으로 설정
                $('#qa-notify').attr('data-notify', '0'); // 오류 발생 시 0으로 설정
            }
        });
    }


    $(document).ready(function () {

        updateCount();

    });
</script>



</html>
