<%@ page import="com.githrd.figurium.product.entity.Products, java.util.List" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <title>주문/결제</title>
    <link rel="icon" type="image/png" href="/images/FiguriumHand.png"/>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
  <link rel="stylesheet" href="/css/orderForm.css">
  <%-- 자바스크립트 경로 --%>
  <script src="/js/orderForm.js"></script>

  <!-- SweetAlert2 -->
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/sweetalert2@11.4.10/dist/sweetalert2.min.css">
  <script src="https://cdn.jsdelivr.net/npm/sweetalert2@11.4.10/dist/sweetalert2.min.js"></script>

  <%-- 결제 API --%>
  <script src="http://code.jquery.com/jquery-latest.min.js"></script>
  <script type="text/javascript"	src="https://cdn.iamport.kr/js/iamport.payment-1.2.0.js"></script>
  <%-- 주소 API --%>
  <script src="//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js"></script>

  <style>
    .table {
      max-width: 1400px !important;
    }

    .order_box_l {
      margin-right: 100px !important;
    }
  </style>

  <script>

    $(function () {

      // 로그인 사용자 확인
      if (${empty loginUser}) {
        Swal.fire({
          icon: 'error',
          title: '로그인 필요',
          text: '로그인 후 이용 가능합니다.',
        }).then(() => {
          location.href = "/";
        });
      }
    });

      function check_name() {

        let order_name = $("#order_name").val();

        if(order_name.length==0) {
          $("#id_msg").html("");
          return;
        }

        if(order_name.length<2 || 5<order_name.length) {
          $("#id_msg").html("주문자명이 올바른 형식이 아닙니다.").css("color","red");
          return;
        } else {
          $("#id_msg").html("주문자명이 올바른 형식입니다.").css("color","blue");
          return;
        }

      }

      const phone_pattern = /^01\d{9}$/;
      function check_phone() {

        let order_phone = $("#order_phone").val();


        if(order_phone.length==0) {
          $("#phone_msg").html("");
          return;
        }

        if(phone_pattern.test(order_phone)) {
          $("#phone_msg").html("전화번호가 올바른 형식입니다.").css("color","blue");
          return;
        } else {
          $("#phone_msg").html("전화번호 형식이 올바르지 않습니다.").css("color","red");
          return;
        }

      }

      const email_pattern = /^[a-zA-Z0-9]+@[a-zA-Z0-9]+\.(com|net)$/;
      function check_email() {

        let order_email = $("#order_email").val();

        if(order_email.length==0) {
          $("#email_msg").html("");
          return;
        }

        if(email_pattern.test(order_email)) {
          $("#email_msg").html("이메일이 올바른 형식입니다.").css("color","blue");
          return;
        } else {
          $("#email_msg").html("이메일 형식이 올바르지 않습니다.").css("color","red");
          return;
        }

      }

      function check_name2() {

        let shipping_name = $("#shipping_name").val();

        if(shipping_name.length==0) {
          $("#shipping_name_msg").html("");
          return;
        }

        if(shipping_name.length<2 || 5<shipping_name.length) {
          $("#shipping_name_msg").html("받는 사람이름이 올바른 형식이 아닙니다.").css("color","red");
          return;
        } else {
          $("#shipping_name_msg").html("받는 사람이름이 올바른 형식입니다.").css("color","blue");
          return;
        }

      }

      function check_phone2() {

        let shipping_phone = $("#shipping_phone").val();

        if(shipping_phone.length==0) {
          $("#shipping_phone_msg").html("");
          return;
        }

        if(phone_pattern.test(shipping_phone)) {
          $("#shipping_phone_msg").html("전화번호가 올바른 형식입니다.").css("color","blue");
          return;
        } else {
          $("#shipping_phone_msg").html("전화번호 형식이 올바르지 않습니다.").css("color","red");
          return;
        }

      }




  // 결제 api js 파일로 분리해놓으면 IMP 못읽어오는 현상이 있어서, 부득이하게 jsp 내부에 js 작성
  // 관리자 계정 정보 (결제 api 사용에 필요함)
  var IMP = window.IMP;
  IMP.init("imp25608413");

    const Toast = Swal.mixin({
      toast: true,
      position: 'center-center',
      showConfirmButton: false,
      timer: 3000,
      timerProgressBar: true,
      didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer)
        toast.addEventListener('mouseleave', Swal.resumeTimer)
      }
    })

  var merchantUid;

    function buyItems() {

      // 결제 주문자, 배송지 정보 유효한지 검증(Test시, 꺼놓는걸 추천)
      let order_name = $("#order_name").val();
      let order_phone = $("#order_phone").val();
      let order_email = $("#order_email").val();
      if(order_name == "" || order_phone == "" || order_email == "" || !phone_pattern.test(order_phone)
              || !email_pattern.test(order_email)) {
        Swal.fire({
          icon: 'error',
          title: '알림',
          text: '주문자 정보는 필수적으로 기입해야 합니다.',
          confirmButtonText: '확인'
        }); // 결제검증이 실패하면 이뤄지는 실패 로직
        return;
      }

      let shipping_name = $("#shipping_name").val();
      let shipping_phone = $("#shipping_phone").val();
      let address = $("#address").val();
      let mem_zipcode1 = $("#mem_zipcode1").val();
      let mem_zipcode2 = $("#mem_zipcode2").val();
      if(shipping_name == "" || shipping_phone == ""
              || address == "" || mem_zipcode1 == "" || mem_zipcode2 == ""
              || !phone_pattern.test(shipping_phone)) {
        Swal.fire({
          icon: 'error',
          title: '알림',
          text: '배송지 정보는 필수적으로 기입해야 합니다.',
          confirmButtonText: '확인'
        }); // 결제검증이 실패하면 이뤄지는 실패 로직
        return;
      }
      // 결제 주문자, 배송지 정보 유효한지 검증(Test시, 꺼놓는걸 추천)





      // 만약에 결제 방식을 선택하지 않았다면, return되게 한다.
      let paymentType = $("input[name='payment']:checked").val();
      if (paymentType == null) {
        Swal.fire({
          icon: 'info',
          title: '알림',
          text: '결제방식을 선택하고 결제를 진행해주세요.',
          confirmButtonText: '확인'
        }); // 결제검증이 실패하면 이뤄지는 실패 로직
        return;
      }
      // 약관 동의 체크
      let agreementCheckbox = document.getElementById("agreement");
      if(!agreementCheckbox.checked) {
        Swal.fire({
          icon: 'info',
          title: '알림',
          text: '약관에 동의하여야 결제를 진행할 수 있습니다.',
          confirmButtonText: '확인'
        }); // 결제검증이 실패하면 이뤄지는 실패 로직
        return;
      }

      console.log(productIds);
      console.log(itemQuantities);

      $.ajax({
        type : "POST",
        url : "checkProduct.do",
        data : {
          productIds : productIds,
          itemQuantities : itemQuantities
        },
        success: function(res_data){
          Toast.fire({
            icon: 'success',
            title: '' +
                    '<img src="/images/흰둥이.png" alt="흰둥이" style="width: 200px; height: auto;">' +
                    '<br>주문이 정상적으로 요청되었습니다.'
          })

          setTimeout(function () {


          IMP.request_pay({
            pg: 'kcp', // PG사 코드표에서 선택
            pay_method: paymentType, // 결제 방식
            merchant_uid: 'merchant_' + new Date().getTime(), // 결제 고유 번호
            name: '피규리움 결제창',   // 상품명
            // <c:set var="amount" value="${ totalPrice < 100000 ? totalPrice + 3000 : totalPrice}"/>
            // amount: <c:out value="${amount}" />, // 가격
            amount: 200, // 가격
            buyer_email: $("#order_email").val(),
            buyer_name: '피규리움 기술지원팀',
            buyer_tel: $("#order_phone").val(),
            buyer_addr: $("#address").val() + $("#mem_zipcode1").val() + $("#mem_zipcode2").val(),
            buyer_postcode: '123-456'
          }, function (rsp) { // callback
            console.log(rsp);


            // 결제검증
            $.ajax({
              type : "GET",
              url  : "/verifyIamport/" + rsp.imp_uid + "?totalPrice=" + ${ totalPrice } +
                      "&merchantUid=" + rsp.merchant_uid
              // data: {
              //   itemPrices: itemPrices, // productPrice 추가
              //   itemQuantities: itemQuantities // productQuntity 추가
              // },
              // dataType: "json"
            }).done(function(data){
              console.log(data);

              merchantUid = rsp.merchant_uid;

              // 위의 rsp.paid_amount(결제 완료 후 객체 정보를 JSON으로 뽑아옴)와
              // data.response.amount(서버에서 imp_uid로 iamport에 요청된 결제 정보)를 비교한후 로직 실행
              if(true) {
                sil();
              } else {
                Swal.fire({
                  icon: 'error',
                  title: '결제 실패',
                  text: '결제에 실패했습니다. 관리자에게 문의해주세요.',
                  confirmButtonText: '확인'
                }); // 결제검증이 실패하면 이뤄지는 실패 로직
              }
            });
          });
        }, 2500);
        },
        error: function(err){
          alert("해당 상품의 재고가 충분하지 않습니다. 해당 상품의 재고를 문의해주세요.");
          return;
        }
      });


    }


    function sil() {

      let paymentType = $("input[name='payment']:checked").val();
      let userId = document.getElementById("order_id").value;    // 보낸 사람 id

      console.log(paymentType);

      //결제 완료된 주문 데이터 저장
      $.ajax({
        type : "POST",
        url  : "/order/inicisPay.do",
        data : {
          price: <c:out value="${totalPrice+3000}" />,
          paymentType: paymentType,
          userId: userId,
          merchantUid: merchantUid
        },

        success: function (res_data){
          insertInformation();
        },

        error: function(err){
          alert(err.responseText);
        }
      });

    }


    function insertInformation() {

      // 주문 리스트에 저장될 값들 전부 변수로 저장

      // var itemNames = [ 아이템 이름 배열 저장 ];
      // var itemPrices = [ 아이템 가격 배열 저장 ];
      // var itemQuantities = [ 아이템 갯수 배열 저장 ];

      let loginUserId = document.getElementById("order_id").value;    // 보낸 사람 id
      let name = document.getElementById("order_name").value;         // 보낸 사람 이름
      let phone = document.getElementById("order_phone").value;       // 보낸 사람 전화번호
      let email = document.getElementById("order_email").value;       // 이메일


      // 받는 사람 주소
      let memZipcode0 = document.getElementById('address').value;
      let memZipcode1 = document.getElementById('mem_zipcode1').value;
      let memZipcode2 = document.getElementById('mem_zipcode2').value;

      let address = memZipcode0 + ' ' + memZipcode1 + ' ' + memZipcode2;

      let recipientName = document.getElementById("shipping_name").value;         // 받는 사람 이름
      let shippingPhone = document.getElementById("shipping_phone").value;       // 받는 사람 주소
      let deliveryRequest = document.getElementById("delivery_request").value;   // 배송 요청 사항


      console.log(address);

  /*    let shipping_address = f.shipping_address.value;  // 배송지
      let shipping_name = f.shipping_name.value;        // 받는 사람
      let shipping_phone = f.shipping_phone.value;      // 받는 사람 전화번호
      let delivery_request = f.delivery_request.value;  // 배송시 요청사항*/


      $.ajax({
        type : "POST",
        url : "insertInformation.do",
        data : {
          loginUserId : loginUserId,
          name : name,
          phone : phone,
          email : email,
          address : address,
          recipientName : recipientName,
          shippingPhone : shippingPhone,
          deliveryRequest : deliveryRequest,
          productIds : productIds,
          itemPrices : itemPrices,
          itemQuantities : itemQuantities
/*          shipping_address : shipping_address,
          paymentType : paymentType,
          itemNames : itemNames,
          itemPrices : itemPrices,
          itemQuantities : itemQuantities*/
        },
        success: function(res_data){
          Toast.fire({
            icon: 'success',
            title: '주문이 정상적으로 처리되었습니다.'
          });

          // 2초 후에 페이지 이동
          setTimeout(function () {
            location.href="../user/order-list.do";
          }, 2500);
        },
        error: function(err){
          alert(err.responseText);
        }
      });


    }
  </script>


</head>
<body class="animsition">
<jsp:include page="../common/header.jsp"/>
<div style="height: 90px"></div>










<%-- Content --%>

<div id="content_title">

  <div class="cart_list" style="margin: 20px;">
    <!-- breadcrumb -->
    <div class="container">
      <div class="bread-crumb flex-w p-l-25 p-r-15 p-t-30 p-lr-0-lg">
        <a href="../" class="stext-109 cl8 hov-cl1 trans-04">
          Home
          <i class="fa fa-angle-right m-l-9 m-r-10" aria-hidden="true"></i>
        </a>

        <a href="CartList.do?loginUser=${ loginUser.id }" class="stext-109 cl8 hov-cl1 trans-04">
          장바구니
          <i class="fa fa-angle-right m-l-9 m-r-10" aria-hidden="true"></i>
        </a>

        <span class="stext-109 cl4">
          주문/결제
        </span>
      </div>
    </div>
  </div>

  <h1>주문서</h1>

<%-- 상단 아이템 주문 리스트 --%>
    <%--  장바구니 예시 테이블 : 0828 --%>
  <c:if test="${ cartsList == null }">
  <div class="item_list">
    <table class="table item_list_table table-hover">
      <thead id="thead">
      <tr class="table-light">
        <th class="item_list_table_name">상품명</th>
        <th>가격</th>
        <th>수량</th>
        <th>총 금액</th>
      </tr>
      </thead>
      <tbody>
      <tr class="table_content">
        <td class="table_content_img"><img src="/images/example.jpg" alt="IMG">
        [25년2월입고] 최애의 아이 2기 반프레스토 아쿠아 토우키ver
        </td>
        <td>10,000원</td>
        <td>1</td>
        <td>10,000원</td>
      </tr>
      </tbody>
    </table>
  </c:if>

    <%-- form 시작 지점 --%>
    <form>

    <%-- 만약에 장바구니에 담겼던 item 값이 넘어왔다면 list에 호출 : 0828 --%>
    <%-- itemNames라는 배열을 생성해서 for문안에 넣어 이름을 추가 --%>
    <c:if test="${ requestScope.cartsList != null }">
      <script type="text/javascript">

        let productIds = [];
        let itemPrices = [];
        let itemQuantities = [];

        <c:forEach var="item" items="${ requestScope.cartsList }">
        productIds.push("${ item.productId }");
        itemPrices.push("${ item.price }");
        itemQuantities.push("${ item.quantity }");
        </c:forEach>

      </script>


      <table class="table item_list_table">
          <thead>
          <tr class="table-light">
            <th class="item_list_table_name">상품명</th>
            <th>가격</th>
            <th>수량</th>
            <th>총 금액</th>
          </tr>
          </thead>

        <tbody>
        <c:forEach var="item" items="${ requestScope.cartsList }">
          <tr class="table_content">
            <td class="table_content_img"><img src="${ item.imageUrl }" alt="IMG">
              <span class="table_content_img_text">${ item.name }</span>
            </td>
            <td><fmt:formatNumber type="currency" value="${ item.price }" currencySymbol=""/>원</td>
            <td>${ item.quantity }</td>
            <td><fmt:formatNumber type="currency" value="${ item.price * item.quantity }" currencySymbol=""/>원</td>
          </tr>
        </c:forEach>
        </tbody>
      </table>

    </c:if>



<%--    <c:if test="${ requestScope.cartsList.size() < 2 }">--%>
<%--      <script type="text/javascript">--%>
<%--        let productIds = ${ item.productId };--%>
<%--        let itemPrices = ${ item.price };--%>
<%--        let itemQuantities = ${ item.quantity };--%>
<%--      </script>--%>


<%--      <table class="table item_list_table">--%>
<%--        <thead>--%>
<%--        <tr class="table-light">--%>
<%--          <th class="item_list_table_name">상품명</th>--%>
<%--          <th>가격</th>--%>
<%--          <th>수량</th>--%>
<%--          <th>총 금액</th>--%>
<%--        </tr>--%>
<%--        </thead>--%>

<%--        <tbody>--%>
<%--          <tr class="table_content">--%>
<%--            <td class="table_content_img"><img src="${ cartsList.imageUrl }" alt="IMG">--%>
<%--              <span class="table_content_img_text">${ cartsList.name }</span>--%>
<%--            </td>--%>
<%--            <td><fmt:formatNumber type="currency" value="${ cartsList.price }" currencySymbol=""/>원</td>--%>
<%--            <td>${ cartsList.quantity }</td>--%>
<%--            <td><fmt:formatNumber type="currency" value="${ cartsList.price * cartsList.quantity }" currencySymbol=""/>원</td>--%>
<%--          </tr>--%>
<%--        </tbody>--%>
<%--      </table>--%>
<%--    </c:if>--%>



</div>



<div class="order_box_both">
  <input type="hidden" value="${ sessionScope.loginUser.id }" id="order_id">

<%-- 주문 테이블 customers --%>
<div class="order_box_l mt-3">
  <div class="form_container">
    <table class="table">
      <thead>
      <th>
        <h2>주문자 입력</h2>
      </th>
      </thead>
      <tbody>
      <tr>
        <td class="td_title">주문하시는 분</td>
        <td>
          <input type="text" class="form-control" value="${ sessionScope.loginUser.name }"
                   id="order_name" placeholder="주문하시는 분" name="order_name" onkeyup="check_name();">
          <span id="id_msg"></span>
        </td>
      </tr>
      <tr>
        <td class="td_title">전화번호</td>
        <td>
          <input type="text" class="form-control" value="${ sessionScope.loginUser.phone }"
                   id="order_phone" placeholder="전화번호" name="order_phone" onkeyup="check_phone();">
          <span id="phone_msg"></span>
        </td>
      </tr>
      <tr>
        <td class="td_title">이메일</td>
        <td><input type="email" class="form-control" value="${ sessionScope.loginUser.email }"
                   id="order_email" placeholder="이메일" name="order_email" onkeyup="check_email();">
          <span id="email_msg"></span>
        </td>
      </tr>
      </tbody>
    </table>
  </div>

  <div id="table_under_box">
    <span>회원정보가 변경되셨다면 다음 버튼을 누르고 수정해주세요.</span>
    <input type="button" class="form-control" onclick="location.href='/user/my-page.do'"
           id="user_change_btn" value="회원정보수정">
  </div>

  <%-- 주문 테이블 shipping_address --%>
  <div class="form_container">
    <table class="table">
      <thead>
      <th>
        <h2>배송지 정보</h2>
      </th>
      </thead>
      <tbody>
      <tr>
        <td class="td_title">기존 배송지</td>
        <td><input type="text" class="form-control" value="${ sessionScope.loginUser.address }"
                   id="shipping_address" placeholder="기본 배송지" name="shipping_address">
        </td>
      </tr>
      <tr>
        <td class="td_title">받으시는 분</td>
        <td><input type="text" class="form-control" value="${ sessionScope.loginUser.name }"
                   id="shipping_name" placeholder="받으시는 분" name="shipping_name" onkeyup="check_name2();">
          <span id="shipping_name_msg"></span>
        </td>
      </tr>
      <tr>
        <td class="td_title">전화번호</td>
        <td><input type="number" class="form-control" value="${ sessionScope.loginUser.phone }"
                   id="shipping_phone" placeholder="전화번호" name="shipping_phone" onkeyup="check_phone2();">
          <span id="shipping_phone_msg"></span>
        </td>
      </tr>


      <tr>
        <td class="td_title">주소</td>
        <td>
          <div class="address-container">
            <div class="address-inputs">
              <input type="text" class="form-control" id="address" placeholder="우편번호" name="address">
              <button id="a_search" type="button" onclick="find_addr();">우편번호 찾기</button>
            </div>
            <div class="zipcode-container">
              <input type="text" class="form-control addr_text" name="mem_zipcode" id="mem_zipcode1" placeholder="주소">
              <input type="text" class="form-control addr_text" name="mem_zipcode" id="mem_zipcode2" placeholder="상세주소">
            </div>
          </div>
        </td>
      </tr>


      <tr>
        <td class="td_title">배송시요청사항</td>
        <td>
          <textarea class="form-control" rows="5" id="delivery_request" placeholder="배송시 요청사항" placeholer="배송시 요청사항을 적어주세요."></textarea>
        </td>
        <%--<td><textarea class="form-control" id="delivery_request" placeholder="배송시 요청사항" name="delivery_request"></td>--%>
      </tr>
      </tbody>
    </table>
  </div>
</div>

  </form>
  <%-- form end 지점 --%>

  <div id="order_box">

      <div class="payment-title">결제 정보</div>

      <%-- 상품가격 + 배송비 계산 항목 : 0828 --%>
      <c:if test="${ cartsList == null }">
      <div class="payment-info">
        <span>상품 합계</span>
        <span class="payment-info-price">0원</span>
      </div>

      <div class="payment-info">
        <span>배송료</span>
        <span class="payment-info-price">(+)0원</span>
      </div>

      <div class="payment-info" id="payment-info-bottom">
        <span>총 결제 금액</span>
        <span class="payment-info-price-red">0원</span>
      </div>
    </c:if>

    <c:if test="${ cartsList != null }">
      <div class="payment-info">
        <span>상품 합계</span>
        <span class="payment-info-price">
          <fmt:formatNumber type="currency" value="${ totalPrice }" currencySymbol=""/>원
        </span>
      </div>

      <div class="payment-info">
        <span>배송료</span>
        <c:if test="${ totalPrice < 100000 }">
          <span class="payment-info-price">(+)3,000원</span>
        </c:if>
        <c:if test="${ totalPrice >= 100000 }">
          <span class="payment-info-price glowing-text">0원 (배송비 무료 이벤트 적용!)</span>
        </c:if>
      </div>

      <div class="payment-info" id="payment-info-bottom">
        <span>총 결제 금액</span>

        <span class="payment-info-price-red">
          <c:set var="finalValue" value="${ totalPrice < 100000 ? totalPrice + 3000 : totalPrice}"/>
          <fmt:formatNumber type="currency" value="${finalValue}" currencySymbol=""/>원
        </span>
      </div>
    </c:if>

    <hr id="hr1">

    <%--  결제 수단 정렬  --%>
    <div class="payment-method">
      <div class="payment-method-title">결제 수단</div>

      <div class="payment-option">
        <input type="radio" id="credit_card" name="payment" value="card">
        <label for="credit_card">통합결제</label>
      </div>

      <div class="payment-option">
        <input type="radio" id="bank_transfer" name="payment" value="vbank">
        <label for="bank_transfer">무통장 입금</label>
      </div>


    </div>

    <hr id="hr2" style="margin-top: 30px;">

    <div class="agreement">
      <input type="checkbox" id="agreement">
      <p>결제 정보를 확인하였으며,<br>구매 진행에 동의합니다.</p>
    </div>

    <%--  결제버튼  --%>
    <button class="order-button" onclick="buyItems();">주문하기</button>

    <div class="info-section" style="display: flex; align-items: center; margin-top: 50px;">
      <img src="/images/신태일.png" alt="신태일.png" style="width: 100px; height: auto; margin-left: 20px;">
      <div>
        <h1 style="font-size: 24px; margin: 0; text-align: left">잠깐만요!</h1>
        <p style="padding-top: 10px;">피규리움에서는 10만원 이상 결제시, 택배비가 무료!</p>
        <p>택배비는 저희가 책임질게요!</p>
      </div>
    </div>

  </div>

</div>

<!-- NOTE : 푸터바 -->
<jsp:include page="../common/footer.jsp"/>


</body>
</html>