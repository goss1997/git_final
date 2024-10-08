package com.githrd.figurium.product.controller;

import com.githrd.figurium.common.page.CommonPage;
import com.githrd.figurium.common.page.Paging;
import com.githrd.figurium.common.s3.S3ImageService;
import com.githrd.figurium.common.session.SessionConstants;
import com.githrd.figurium.exception.customException.ProductNotFoundException;
import com.githrd.figurium.product.dao.CartsMapper;
import com.githrd.figurium.product.dao.ProductsMapper;
import com.githrd.figurium.product.entity.Category;
import com.githrd.figurium.product.entity.Products;
import com.githrd.figurium.product.repository.CategoriesRepository;
import com.githrd.figurium.product.service.CartService;
import com.githrd.figurium.product.service.CartServiceImpl;
import com.githrd.figurium.product.service.ProductsService;
import com.githrd.figurium.product.vo.ProductsVo;
import com.githrd.figurium.productLike.service.ProductLikeService;
import com.githrd.figurium.qa.dao.QaMapper;
import com.githrd.figurium.qa.vo.QaVo;
import com.githrd.figurium.reviews.service.ReviewService;
import com.githrd.figurium.reviews.vo.ReviewVo;
import com.githrd.figurium.user.entity.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProductsController {

    private final ProductsService productsService;
    private final ReviewService reviewService;
    private final CategoriesRepository categoriesRepository;
    private final HttpSession session;
    private final ProductLikeService productLikeService;
    private final S3ImageService s3ImageService;
    private final QaMapper qaMapper;
    private final CartsMapper cartsMapper;
    private final ProductsMapper productsMapper;
    private final CartService cartService;

    // 해당 카테고리의 필터 처리와 페이징 처리
    @GetMapping("/productList.do")
    public String productList(@RequestParam(defaultValue = "all") String selectFilter,
                              @RequestParam(value = "name") String categoryName,
                              @RequestParam(defaultValue = "1") int page,
                              Model model) {

        int pageSize = 80;
        int offset = (page - 1) * pageSize;

        // filter와 name을 같이 받아오기 위해 맵에다가 저장
        Map<String, Object> params = new HashMap<>();
        params.put("selectFilter", selectFilter);
        params.put("categoryName", categoryName);
        params.put("offset", offset);
        params.put("pageSize", pageSize);

        /**
         *  해당 카테고리의 리스트를 동적 쿼리를 사용해 가져옴
         * */
        List<ProductsVo> productCategoriesList = productsService.categoriesList(params, page, pageSize);

        model.addAttribute("productCategoriesList", productCategoriesList);
        model.addAttribute("categoryName", categoryName); // 필터 처리를 위해 모델에 담음
        model.addAttribute("selectFilter", selectFilter); // 현재 필터링 된 option을 보여주기 위해 모델에 담음

        /**
         *  해당 카테고리를 페이징 처리 하기 위해 현재 카테고리 목록의 갯수를 가져옴
         * */
        int totalCount = productsService.categoriesProductsCount(params);
        model.addAttribute("totalCount", totalCount);

        // 총 페이지 수 계산
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page); // 현재 페이지 정보

        /* 페이지 버튼 범위 계산
            현재 페이지에서 2페이지 이전이 최소 1페이지보다 작을 경우, 시작 페이지를 1로 설정 */
        int startPage = Math.max(1, page - 2); // 시작 페이지

        // 현재 페이지에서 2페이지 이후가 총 페이지 수보다 클 경우, 끝 페이지를 총 페이지 수로 설정
        int endPage = Math.min(totalPages, page + 2); // 끝 페이지

        // 이전 및 다음 버튼 페이지 범위 계산
        int prevPage = Math.max(1, page - 5); // 이전 버튼을 누를 경우 이동할 페이지
        int nextPage = Math.min(totalPages, page + 5); // 다음 버튼을 누를 경우 이동할 페이지

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("prevPage", prevPage); // 이전 버튼 페이지 번호
        model.addAttribute("nextPage", nextPage); // 다음 버튼 페이지 번호

        return "products/productCategories";
    }

    /**
     * No Offset 페이징 처리
     */
    @GetMapping("/load-more-products")
    @ResponseBody
    public List<ProductsVo> loadMoreProducts(
            @RequestParam(value = "lastCreatedAt", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastCreatedAt,
            @RequestParam(value = "lastPrice", required = false) Integer lastPrice,
            @RequestParam(value = "lastLikeCount", required = false) Integer lastLikeCount,
            @RequestParam(value = "lastId", required = false) Integer lastId,
            @RequestParam(value = "categoryName", required = false) String categoryName,
            @RequestParam(value = "selectFilter", required = false, defaultValue = "newProducts") String selectFilter) {

        Map<String, Object> params = new HashMap<>();
        params.put("lastCreatedAt", lastCreatedAt);
        params.put("lastPrice", lastPrice);
        params.put("lastId", lastId);
        params.put("categoryName", categoryName);
        params.put("selectFilter", selectFilter);
        params.put("lastLikeCount", lastLikeCount);

        return productsService.getNextPageByCreatedAt(params);

    }


    @RequestMapping("/productInfo.do")
    public String list(@RequestParam(value = "id", required = false) Integer id,
                       @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "showQa", defaultValue = "false") String showQa,
                       HttpSession session,
                       RedirectAttributes ra,
                       Model model, Map map) {

        // 해당 상품에 해당하는 ID를 받아옴
        try {
            Products selectOne = productsService.getProductById(id);
            model.addAttribute("product", selectOne);
        } catch (ProductNotFoundException e) {
            // 상품이 없을 경우 예외 발생
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/"; // 상품이 없을 경우 리다이렉트할 페이지
        }

        // 페이지 설정
        int pageSize = 5;
        int offset = (page - 1) * pageSize;


        // 리뷰 리스트 및 리뷰 개수 가져오기
        List<ReviewVo> reviewList = reviewService.getReviewsWithPagination(id, offset, pageSize);
        model.addAttribute("reviewList", reviewList);


        // 해당 상품에 대한 ID 값을 이용해 리뷰의 갯수를 가져옴
        int reviewCount = reviewService.reviewCountByProductId(id);
        model.addAttribute("reviewCount", reviewCount);

        // 페이지 관련 정보 설정
        int totalPages = (int) Math.ceil((double) reviewCount / pageSize);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page);


        // 해당 상품의 평균 별점 가져오기
        int ratingAvg = reviewService.reviewRatingAvg(id);
        model.addAttribute("ratingAvg", ratingAvg);

        // 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute(SessionConstants.LOGIN_USER);
        if (user != null) {
            boolean isLiked = productLikeService.isProductLikedByUser(id, user.getId());
            model.addAttribute("isLiked", isLiked);
        } else {
            model.addAttribute("isLiked", false);
        }

        // Q&A 리스트와 관련된 페이징 설정
        // Q&A 관련 정보 설정
        // Q&A 리스트 및 페이징 처리
        if (true) {
            int qaPageSize = 5;
            int qaOffset
                    = (page - 1) * qaPageSize;

            Map<String, Object> qaMap = new HashMap<>();
            qaMap.put("start", qaOffset + 1);

            qaMap.put("end", qaOffset + qaPageSize);

            qaMap.put("productId", id);

            List<QaVo> productQaList = qaMapper.selectProductAllWithPagination(qaMap);

            int productQaCount = qaMapper.getProductQaCount(id);

            String qaPageMenu = Paging.getPaging("productInfo.do",
                    page,
                    productQaCount,
                    qaPageSize,

                    CommonPage.qaList.BLOCK_PAGE);

            model.addAttribute("qaPageMenu", qaPageMenu);
            model.addAttribute("productQaList", productQaList);
            model.addAttribute("productQaCount", productQaCount);
            model.addAttribute("showQa", true);
        } else {
            model.addAttribute("showQa", false);
        }

        return "products/productInfo";
    }

    @GetMapping("/productInsertForm.do")
    public String productInsertForm(Model model) {

        User loginUser = (User) session.getAttribute(SessionConstants.LOGIN_USER);

        if (loginUser == null || loginUser.getRole() != 1) {
            session.setAttribute(SessionConstants.ALERT_MSG, "관리자만 접속이 가능합니다.");
            return "redirect:/";
        }

        List<Category> categoriesList = categoriesRepository.findAll();
        model.addAttribute("categoriesList", categoriesList);

        return "products/productInsertForm";
    }

    @RequestMapping("/productInsert.do")
    public String productInsert(ProductsVo products, @RequestParam MultipartFile productImage) {


        String save;
        if (productImage.isEmpty()) {

            products.setImageUrl("/images/noImage1.png");

            int res = productsService.productSave(products);

            if (res > 0) {
                System.out.println("저장실패");
                return "redirect:/"; // 저장 실패 시 리다이렉션
            } else {
                System.out.println("등록성공");
                return "redirect:/"; // 저장 성공 시 리다이렉션
            }

        }

        save = productsService.imageSave(products, productImage);


        if (save == null) {
            System.out.println("저장실패");
            return "redirect:/"; // 저장 실패 시 리다이렉션
        } else {
            System.out.println("등록성공");
            return "redirect:/"; // 저장 성공 시 리다이렉션
        }
    }


    @RequestMapping("/productModifyForm.do")
    public String productModifyForm(@RequestParam(value = "id", required = false) Integer id,
                                    Model model) {

        User loginUser = (User) session.getAttribute(SessionConstants.LOGIN_USER);

        if (loginUser == null || loginUser.getRole() != 1) {
            session.setAttribute(SessionConstants.ALERT_MSG, "관리자만 접속이 가능합니다.");
            return "redirect:/";
        }

        // 해상 상품에 해당하는 ID를 받아옴
        Products selectOne = productsService.getProductById(id);
        List<Category> categoriesList = categoriesRepository.findAll();
        model.addAttribute("product", selectOne);
        model.addAttribute("categoriesList", categoriesList);

        return "products/productModifyForm";
    }

    @RequestMapping("/productModify.do")
    public String productModify(ProductsVo products, @RequestParam MultipartFile productImage) {


        Products productById = productsService.getProductById(products.getId());
        String oldImageUrl = productById.getImageUrl();
        int save = 0;


        System.out.println("기존 이미지 : " + oldImageUrl);
        System.out.println("새로운 이미지 : " + productImage);



        boolean isNewImageEmpty = (productImage == null || productImage.isEmpty());
        boolean isOldImageNoImage = oldImageUrl.equals("/images/noImage1.png");
        System.out.println(isNewImageEmpty);
        System.out.println(isOldImageNoImage);

        if (oldImageUrl.equals("null") || oldImageUrl.isEmpty()){
            products.setImageUrl("/images/noImage1.png");
        }
        // 1. 새로운이미지 X / 기존이미지 X   == 추가 작업 x
        if (isNewImageEmpty && isOldImageNoImage) {
            System.out.println("1. 새로운이미지 X / 기존이미지 X   == 추가 작업 x");
            products.setImageUrl("/images/noImage1.png");
            save = productsMapper.productUpdate(products);

        // 2. 새로운이미지 X / 기존이미지 O   == 추가 작업 x
        } else if (isNewImageEmpty) {
            System.out.println("2. 새로운이미지 X / 기존이미지 O   == 추가 작업 x");
            products.setImageUrl(oldImageUrl);
            save = productsMapper.productUpdate(products);

        // 3. 새로운이미지 O / 기존이미지 X
        } else if (!isNewImageEmpty && isOldImageNoImage) {
            System.out.println("3. 새로운이미지 O / 기존이미지 X   == 기존이미지 db에서만 교체 + 새로운이미지 s3넣고 db교체");
            products.setImageUrl(s3ImageService.upload(productImage));
            save = productsMapper.productUpdate(products);

        // 4. 새로운이미지 O / 기존이미지 O
        } else if (!isNewImageEmpty && !isOldImageNoImage) {
            System.out.println("4. 새로운이미지 O / 기존이미지 O   == 기존 이미지 s3 삭제 + 새로운이미지 s3넣고 db교체");
            products.setImageUrl(oldImageUrl);
            save = productsService.updateProductsImage(products, productImage);
        }

        if (save == 0) {
            System.out.println("저장실패");
            session.setAttribute(SessionConstants.ALERT_MSG, "상품 수정이 실패하였습니다.");
            return "redirect:/"; // 저장 실패 시 리다이렉션
        } else {
            System.out.println("등록성공");
            session.setAttribute(SessionConstants.ALERT_MSG, "상품 수정이 성공하였습니다.");
            return "redirect:/productInfo.do?id=" + products.getId(); // 저장 성공 시 리다이렉션
        }


    }


    @PostMapping("/productDelete.do")
    public String productDeleteById(@RequestParam int id) {

        User loginUser = (User) session.getAttribute(SessionConstants.LOGIN_USER);

        if (loginUser == null || loginUser.getRole() != 1) {
            session.setAttribute(SessionConstants.ALERT_MSG, "관리자만 접속이 가능합니다.");
            return "redirect:/";
        }

        Products selectOne = productsService.getProductById(id);

        productsService.productDeleteUpdate(selectOne);

        // 장바구니 담았던 유저에게 삭제 알람 발송
        cartService.productDeleteAlram(id);

        // 장바구니에 있는 삭제된 상품 모두 삭제
        cartsMapper.deleteCartProductAll(id);



        return "redirect:/";

    }


    // 검색한 상품에 대한 필터 처리와 페이징 처리
    @GetMapping("/searchProductsList.do")
    public String searchList(@RequestParam(defaultValue = "all") String selectFilter,
                             @RequestParam(value = "search") String search,
                             @RequestParam(defaultValue = "1") int page,
                             Model model) {

        int searchHistorySuccess = productsService.searchProductsNameHistory(search);


        if (searchHistorySuccess < 0) {
            model.addAttribute("not_search", true);
            return "home";
        }


        // 검색 상품이 없을경우
        if (searchHistorySuccess == 0){
            model.addAttribute("not_search" , true);
            return "products/productsSearch";
        }

        // 한 화면에 보여질 상품의 갯수
        int pageSize = 80;
        int offset = (page - 1) * pageSize;

        // filter와 name을 같이 받아오기 위해 맵에다가 저장
        Map<String, Object> params = new HashMap<String, Object>();

        params.put("selectFilter", selectFilter);
        params.put("search", search);
        params.put("offset", offset);
        params.put("pageSize", pageSize);

        // 검색된 상품의 리스트
        List<ProductsVo> productsSearchList = productsService.searchProductsList(params, page, pageSize);

        model.addAttribute("productsSearchList", productsSearchList);
        model.addAttribute("selectFilter", selectFilter); // 현재 필터링 된 option을 보여주기 위해 모델에 담음
        model.addAttribute("search", search); // option을 선택 했을 경우 해당하는 option의 parameter를 다시 넘겨주기 위해 모델에 담음

        // 총 상품 수를 가져와서 페이지네이션 계산
        int totalCount = productsService.searchProductCount(params);
        model.addAttribute("totalCount", totalCount);

        // 총 페이지 수 계산
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

        model.addAttribute("totalPages", totalPages);
        model.addAttribute("currentPage", page); // 현재 페이지 정보

        /* 페이지 버튼 범위 계산
            현재 페이지에서 2페이지 이전이 최소 1페이지보다 작을 경우, 시작 페이지를 1로 설정 */
        int startPage = Math.max(1, page - 2); // 시작 페이지

        // 현재 페이지에서 2페이지 이후가 총 페이지 수보다 클 경우, 끝 페이지를 총 페이지 수로 설정
        int endPage = Math.min(totalPages, page + 2); // 끝 페이지

        // 이전 및 다음 버튼 페이지 범위 계산
        int prevPage = Math.max(1, page - 5); // 이전 버튼을 누를 경우 이동할 페이지
        int nextPage = Math.min(totalPages, page + 5); // 다음 버튼을 누를 경우 이동할 페이지

        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);
        model.addAttribute("prevPage", prevPage); // 이전 버튼 페이지 번호
        model.addAttribute("nextPage", nextPage); // 다음 버튼 페이지 번호

        return "products/productsSearch";
    }

    // 상품의 검색어 순위
    @RequestMapping("searchRank")
    @ResponseBody
    public List<String> searchRank() {
        return productsService.searchHistory();
    }


}


