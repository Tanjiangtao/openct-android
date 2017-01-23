package cc.metapro.openct.data.university;

/*
 *  Copyright 2016 - 2017 OpenCT open source class table
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.metapro.openct.data.source.StoreHelper;
import cc.metapro.openct.utils.Constants;
import cc.metapro.openct.utils.HTMLUtils.Form;
import cc.metapro.openct.utils.HTMLUtils.FormHandler;
import cc.metapro.openct.utils.HTMLUtils.FormUtils;
import okhttp3.ResponseBody;
import retrofit2.Response;

@Keep
public abstract class UniversityFactory {

    private static final Pattern LOGIN_SUCCESS = Pattern.compile("(当前借阅)|(个人信息)");

    UniversityInfo.LibraryInfo mLibraryInfo;

    UniversityInfo.CMSInfo mCMSInfo;

    LibraryFactory.BorrowTableInfo mBorrowTableInfo;

    CmsFactory.ClassTableInfo mClassTableInfo;

    CmsFactory.GradeTableInfo mGradeTableInfo;

    String dynPart;
    UniversityService mService;
    private boolean gotDynPart;

    @Nullable
    String login(@NonNull Map<String, String> loginMap) throws Exception {
        getDynPart();
        String loginPageHtml = mService.getPage(getLoginURL(), null).execute().body();
        String userCenter = null;

        // 教务网登录
        if (mCMSInfo != null) {
            // 强智教务系统 (特殊处理)
            if (mCMSInfo.mCmsSys.equalsIgnoreCase(Constants.QZDATASOFT)) {
                String serverResponse = mService.login(mCMSInfo.mCmsURL + "Logon.do?method=logon&flag=sess", getLoginURL(), new HashMap<String, String>(0)).execute().body();
                // 加密登录 (模拟网页JS代码)
                String scode = serverResponse.split("#")[0];
                String sxh = serverResponse.split("#")[1];
                String code = loginMap.get(Constants.USERNAME_KEY) + "%%%" + loginMap.get(Constants.PASSWORD_KEY);
                String encoded = "";
                for (int i = 0; i < code.length(); i++) {
                    if (i < 20) {
                        encoded = encoded + code.substring(i, i + 1) + scode.substring(0, Integer.parseInt(sxh.substring(i, i + 1)));
                        scode = scode.substring(Integer.parseInt(sxh.substring(i, i + 1)), scode.length());
                    } else {
                        encoded = encoded + code.substring(i, code.length());
                        i = code.length();
                    }
                }
                Map<String, String> map = new HashMap<>(3);
                map.put("useDogCode", "");
                map.put("encoded", encoded);
                map.put("RANDOMCODE", loginMap.get(Constants.CAPTCHA_KEY));
                Document document = Jsoup.parse(loginPageHtml, mCMSInfo.mCmsURL);
                String action = document.select("form").get(0).absUrl("action");
                userCenter = mService.login(action, getLoginRefer(), map).execute().body();
                if (!TextUtils.isEmpty(userCenter) && LOGIN_SUCCESS.matcher(userCenter).find()) {
                    return userCenter;
                } else {
                    throw new Exception("登录失败, 请检查您的用户名和密码\n(以及验证码)");
                }
            }
        }

        FormHandler handler = new FormHandler(loginPageHtml, getLoginURL());
        Form form = handler.getForm(0);
        if (form == null) {
            throw new Exception("学校服务器好像出了点问题~\n要不等下再试试?");
        }
        Map<String, String> res = FormUtils.getLoginFiledMap(form, loginMap, true);
        String action = res.get(Constants.ACTION);
        res.remove(Constants.ACTION);
        Response<String> stringResponse = mService.login(action, getLoginURL(), res).execute();
        userCenter = stringResponse.body();
        if (userCenter.length() < 100) {
            Thread.sleep(6 * 1000);
            userCenter = mService.login(action, getLoginURL(), res).execute().body();
        }

        // 登录完成, 检测结果
        if (!TextUtils.isEmpty(userCenter) && LOGIN_SUCCESS.matcher(userCenter).find()) {
            return userCenter;
        } else {
            throw new Exception("登录失败, 请检查您的用户名和密码\n(以及验证码)");
        }
    }

    private void getDynPart() {
        if (mCMSInfo != null) {
            if (mCMSInfo.mDynLoginURL && !gotDynPart) {
                try {
                    String dynURL;
                    URL url = new URL(mCMSInfo.mCmsURL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    if (conn.getResponseCode() == 302) {
                        dynURL = conn.getHeaderField("Location");
                        if (!TextUtils.isEmpty(dynURL)) {
                            Pattern pattern = Pattern.compile("\\(.*\\)+");
                            Matcher m = pattern.matcher(dynURL);
                            if (m.find()) {
                                dynPart = m.group();
                            }
                        }
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (!TextUtils.isEmpty(dynPart)) {
                    resetURLFactory();
                    gotDynPart = true;
                }
            }
        }
    }

    public void getCAPTCHA() throws IOException {
        getDynPart();
        Response<ResponseBody> bodyResponse = mService.getCAPTCHA(getCaptchaURL()).execute();
        ResponseBody body = bodyResponse.body();
        StoreHelper.storeBytes(Constants.CAPTCHA_FILE, body.byteStream());
    }

    protected abstract String getCaptchaURL();

    protected abstract String getLoginURL();

    protected abstract String getLoginRefer();

    protected abstract void resetURLFactory();
}
