package com.joy.joymall.member.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.alibaba.fastjson.JSON;
import com.joy.common.exception.BizCodeEnum;
import com.joy.common.utils.HttpUtils;
import com.joy.joymall.member.exception.PhoneAlreadyExistException;
import com.joy.joymall.member.exception.UserNameAlreadyExistException;
import com.joy.joymall.member.feign.CouponFeignService;
import com.joy.joymall.member.vo.IdVo;
import com.joy.joymall.member.vo.MemberLoginVo;
import com.joy.joymall.member.vo.MemberRegisterVo;
import com.joy.joymall.member.vo.SocialUserVo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.joy.joymall.member.entity.MemberEntity;
import com.joy.joymall.member.service.MemberService;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.R;


/**
 * 会员
 *
 * @author dongjoy
 * @email 990937964@qq.com
 * @date 2022-05-04 18:07:36
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponFeignService couponFeignService;

    @RequestMapping("/coupons")
    public R feignTest() {
        MemberEntity entity = new MemberEntity();
        entity.setNickname("小磊");
        R coupon = couponFeignService.memberCoupon();
        return Objects.requireNonNull(R.ok().put("member", entity)).put("coupons", coupon.get("coupons"));
    }

    /**
     * 注册
     */
    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVo vo) {
        try {
            memberService.register(vo);
        } catch (PhoneAlreadyExistException p) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        } catch (UserNameAlreadyExistException u) {
            return R.error(BizCodeEnum.USERNAME_EXIST_EXCEPTION.getCode(), BizCodeEnum.USERNAME_EXIST_EXCEPTION.getMsg());
        }

        return R.ok();
    }

    /**
     * 登录
     */

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo) {

        MemberEntity entity = memberService.login(vo);
        if (entity != null) {
            return R.ok().setData(entity);
        } else {
            return R.error(BizCodeEnum.ACCOUNT_OR_PASSWORD_INVALID_EXCEPTION.getCode(),
                    BizCodeEnum.ACCOUNT_OR_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 社交登录
     */
    @PostMapping("/oauth2/login")
    public R oauthLogin(@RequestBody SocialUserVo vo) throws Exception {

        MemberEntity entity = memberService.login(vo);
        if (entity != null) {
            // TODO 登录后处理
            return R.ok().setData(entity);
        } else {
            return R.error(BizCodeEnum.ACCOUNT_OR_PASSWORD_INVALID_EXCEPTION.getCode(),
                    BizCodeEnum.ACCOUNT_OR_PASSWORD_INVALID_EXCEPTION.getMsg());
        }
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id) {
        MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member) {
        memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member) {
        memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids) {
        memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
