package com.joy.joymall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.joy.common.utils.HttpUtils;
import com.joy.joymall.member.dao.MemberLevelDao;
import com.joy.joymall.member.exception.PhoneAlreadyExistException;
import com.joy.joymall.member.exception.UserNameAlreadyExistException;
import com.joy.joymall.member.vo.IdVo;
import com.joy.joymall.member.vo.MemberLoginVo;
import com.joy.joymall.member.vo.MemberRegisterVo;
import com.joy.joymall.member.vo.SocialUserVo;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joy.common.utils.PageUtils;
import com.joy.common.utils.Query;

import com.joy.joymall.member.dao.MemberDao;
import com.joy.joymall.member.entity.MemberEntity;
import com.joy.joymall.member.service.MemberService;

import javax.annotation.Resource;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Resource
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(MemberRegisterVo vo) {
        MemberEntity memberEntity = new MemberEntity();

        //设置默认等级
        Long defaultLevel = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(defaultLevel);

        //设置其它信息
        //手机号和用户名应该是唯一的
        //创建自定义异常类，如果存在重复则抛异常
        checkMobileUnique(vo.getPhone());
        checkUserNameUnique(vo.getUserName());
        //检查通过
        memberEntity.setMobile(vo.getPhone());
        memberEntity.setUsername(vo.getUserName());
        memberEntity.setNickname(vo.getUserName());
        //密码存储要加密
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encodePassword = encoder.encode(vo.getPassword());
        memberEntity.setPassword(encodePassword);

        // TODO 其它默认信息

        try {
            this.baseMapper.insert(memberEntity);
        } catch (DuplicateKeyException e) {
            // 兜底：并发场景下唯一索引冲突时回滚事务
            throw new PhoneAlreadyExistException();
        }
    }

    @Override
    public void checkMobileUnique(String phone) throws PhoneAlreadyExistException {
        Integer count = this.baseMapper.selectCount(
                new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneAlreadyExistException();
        }
    }

    @Override
    public void checkUserNameUnique(String userName) throws UserNameAlreadyExistException {
        Integer count = this.baseMapper.selectCount(
                new QueryWrapper<MemberEntity>().eq("username", userName));
        if (count > 0) {
            throw new UserNameAlreadyExistException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginAccount = vo.getLoginAccount();
        String password = vo.getPassword();
        MemberEntity entity = this.baseMapper.selectOne(
                new QueryWrapper<MemberEntity>().eq("username", loginAccount)
                        .or().eq("mobile", loginAccount));
        if (entity == null) {  //此用户不存在
            return null;
        } else {
            String dbPassword = entity.getPassword();
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            boolean isMatches = encoder.matches(password, dbPassword);
            if (isMatches) {
                return entity;
            } else {  //密码不匹配
                return null;
            }
        }
    }


    /**
     * 社交登录
     * 具有登录和注册功能，如果有就登录，否则注册
     *
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(SocialUserVo vo) throws Exception {
        //因为gitee需要查询出用户信息才能得到id
        Map<String, String> headers = new HashMap<>();
        Map<String, String> querys = new HashMap<>();
        querys.put("access_token", vo.getAccess_token());
        HttpResponse response = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get",
                headers, querys);
        String responseEntity = EntityUtils.toString(response.getEntity());
        IdVo idVo = JSON.parseObject(responseEntity, IdVo.class);
        //将id再次封装进去
        vo.setUid(String.valueOf(idVo.getId()));
        //uid唯一
        String uid = vo.getUid();

        MemberEntity memberEntity = this.baseMapper.selectOne(new QueryWrapper<MemberEntity>()
                .eq("social_uid", uid));

        if (memberEntity != null) {
            //注册过
            MemberEntity update = new MemberEntity();

            update.setId(memberEntity.getId());
            update.setAccessToken(vo.getAccess_token());
            update.setExpiresIn(vo.getExpires_in());

            this.baseMapper.updateById(update);

            memberEntity.setAccessToken(vo.getAccess_token());
            memberEntity.setExpiresIn(vo.getExpires_in());
            return memberEntity;

        } else {
            //没有注册过
            MemberEntity register = new MemberEntity();
            try {
                //查询用户的其余信息
                JSONObject jsonObject = JSON.parseObject(responseEntity);
                String name = jsonObject.getString("name");
                //头像
                String avatarUrl = jsonObject.getString("avatar_url");
                //....都可以根据json实体获取，因为太麻烦就不封装vo了
                register.setNickname(name);
                register.setCreateTime(new Date());
                register.setHeader(avatarUrl);
            } catch (Exception e) {

            }

            register.setSocialUid(vo.getUid());
            register.setAccessToken(vo.getAccess_token());
            register.setExpiresIn(vo.getExpires_in());
            this.baseMapper.insert(register);

            return register;
        }
    }
}