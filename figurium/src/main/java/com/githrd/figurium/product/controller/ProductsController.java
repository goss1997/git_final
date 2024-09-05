package com.githrd.figurium.product.controller;

import com.githrd.figurium.product.entity.Category;
import com.githrd.figurium.product.entity.Products;
import com.githrd.figurium.product.repository.CategoriesRepository;
import com.githrd.figurium.product.service.ProductsService;
import com.githrd.figurium.product.vo.ProductsVo;
import com.githrd.figurium.productLike.service.ProductLikeService;
import com.githrd.figurium.reviews.service.ReviewService;
import com.githrd.figurium.reviews.vo.ReviewVo;
import com.githrd.figurium.user.entity.User;
import com.githrd.figurium.util.s3.S3ImageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductsController {

    private final ProductsService productsService;
    private final ReviewService reviewService;
    private final CategoriesRepository categoriesRepository;
    private final HttpSession session;
    private final ProductLikeService productLikeService;
    private final S3ImageService s3ImageService;




    @RequestMapping("/productInfo.do")
    public String list(@RequestParam(value = "id" , required = false) Integer id,
                       HttpSession session,
                       Model model) {

        // 해상 상품에 해당하는 ID를 받아옴
        Products selectOne = productsService.getProductById(id);
        model.addAttribute("product", selectOne);

        // 해당 상품에 대한 ID 값을 이용해 리뷰의 리스트를 가져옴
        List<ReviewVo> reviewList = reviewService.reviewsByProductId(id);
        model.addAttribute("reviewList", reviewList);

        // 해당 상품에 대한 ID 값을 이용해 리뷰의 갯수를 가져옴
        int reviewCount = reviewService.reviewCountByProductId(id);
        model.addAttribute("reviewCount", reviewCount);

        // 해당 상품의 평균 별점 가져오기
        int ratingAvg = reviewService.reviewRatingAvg(id);
        model.addAttribute("ratingAvg", ratingAvg);

        // 세션에서 사용자 정보 가져오기
        User user = (User) session.getAttribute("loginUser");
        if (user != null) {
            boolean isLiked = productLikeService.isProductLikedByUser(id, user.getId());
            model.addAttribute("isLiked", isLiked);
        } else {
            model.addAttribute("isLiked", false);
        }
        System.out.println("user = " + user);
        return "products/productInfo";
    }

    @GetMapping("/productInsertForm.do")
    public String productInsertForm(Model model){

        User loginUser = (User) session.getAttribute("loginUser");

        if(loginUser == null || loginUser.getRole() != 1) {
            session.setAttribute("alertMsg","관리자만 접속이 가능합니다.");
            return "redirect:/";
        }

        List<Category> categoriesList = categoriesRepository.findAll();
        model.addAttribute("categoriesList", categoriesList);

        return "products/productInsertForm";
    }

    @RequestMapping("/productInsert.do")
    public String productInsert(ProductsVo products, @RequestParam MultipartFile productImage) {



        String save;
        if(productImage.isEmpty()){

            products.setImageUrl("/resources/images/noImage1.png");

            int res = productsService.productSave(products);

            if (res > 0) {
                System.out.println("저장실패");
                return "redirect:/"; // 저장 실패 시 리다이렉션
            } else {
                System.out.println("등록성공");
                return "redirect:/"; // 저장 성공 시 리다이렉션
            }

        }

        save = productsService.ImageSave(products, productImage);
        if (save == null) {
            System.out.println("저장실패");
            return "redirect:/"; // 저장 실패 시 리다이렉션
        } else {
            System.out.println("등록성공");
            return "redirect:/"; // 저장 성공 시 리다이렉션
        }
    }



    @RequestMapping("/productModifyForm.do")
    public String productModifyForm(@RequestParam(value = "id" , required = false) Integer id,
                                    Model model) {

        User loginUser = (User) session.getAttribute("loginUser");

        if(loginUser == null || loginUser.getRole() != 1) {
            session.setAttribute("alertMsg","관리자만 접속이 가능합니다.");
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
        products.setImageUrl(oldImageUrl);



        int save = productsService.updateProductsImage(products, productImage);

        if (save == 0) {
            System.out.println("저장실패");
            return "redirect:/"; // 저장 실패 시 리다이렉션
        } else {
            System.out.println("등록성공");
            return "redirect:/productInfo.do?id=" + products.getId(); // 저장 성공 시 리다이렉션
        }


    }


    @DeleteMapping("/product/{id}")
    public Object productDeleteById(@PathVariable int id){

        User loginUser = (User) session.getAttribute("loginUser");

        if(loginUser == null || loginUser.getRole() != 1) {
            session.setAttribute("alertMsg","관리자만 접속이 가능합니다.");
            return "redirect:/";
        }

        Products selectOne = productsService.getProductById(id);
        String imageUrl = selectOne.getImageUrl();

        s3ImageService.deleteImageFromS3(imageUrl);


        productsService.deleteById(id);

        return ResponseEntity.noContent().build();

    }


}


