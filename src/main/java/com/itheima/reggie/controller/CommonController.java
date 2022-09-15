package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

/**
 * 文件上传 下载 处理
 */
@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    //文件copy路径
    @Value("${reggie.path}")
    private String basepath;

    /**
     * 文件上传
     * @RequestParam(name = "file") 参数名字不一致  就匹配一下
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        //file  只是一个临时文件,需要转存到指定位置,否则本次请求完成后,临时文件会删除
        log.info("文件上传: {}",file.getOriginalFilename());

        //动态截取原始文件后缀名
        String originalFilename = file.getOriginalFilename(); //abc.jpg
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //使用UUid重新生成文件名, 防止文件名重复,造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        //创建一个目录对象
        File dir = new File(basepath);
        //判断当前目录是否存在
        if (!dir.exists()){
            //如果不存在 则创建一个目录
            dir.mkdirs();
        }

        try {
            //将临时文件转存到其他位置
            file.transferTo(new File(basepath+ fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return R.success(fileName);
    }

    /**
     * 文件下载
     * @param name
     * @param response
     */
    @GetMapping("/download")
    public void download(String name , HttpServletResponse response){

        try {
            //输入流 通过输入流读取文件
            FileInputStream fileInputStream = new FileInputStream(new File(basepath + name));

            //输出流 通过输出流将文件写回浏览器,在浏览器展示图片
            ServletOutputStream outputStream = response.getOutputStream();

            response.setContentType("image/jpeg");

            int len = 0 ;
            byte[] bytes = new byte[1024];
            while ((len = fileInputStream.read(bytes)) != -1){
                outputStream.write(bytes,0,len);
                outputStream.flush();
            }

            //关闭资源
            fileInputStream.close();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
