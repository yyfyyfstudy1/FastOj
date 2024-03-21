package com.fastoj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fastoj.common.ErrorCode;
import com.fastoj.constant.CommonConstant;
import com.fastoj.exception.BusinessException;
import com.fastoj.mapper.CommentMapper;
import com.fastoj.mapper.UserMapper;
import com.fastoj.model.dto.user.UserAddRequest;
import com.fastoj.model.dto.user.UserQueryRequest;
import com.fastoj.model.entity.Comment;
import com.fastoj.model.entity.User;
import com.fastoj.model.entity.UserCode;
import com.fastoj.model.enums.UserRoleEnum;
import com.fastoj.model.vo.LoginUserVO;
import com.fastoj.model.vo.UserVO;
import com.fastoj.service.CommentService;
import com.fastoj.service.UserCodeService;
import com.fastoj.service.UserService;
import com.fastoj.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.fastoj.constant.UserConstant.*;

/**
 * 用户服务实现
 *
 * @author Shier
 */
@Service
@Slf4j
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {


}
