package com.yy.ent;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 含有@好友功能的富文本EditText
 * 主要用于@好友只有之后部分字体背景颜色改变
 * <p>
 * Created by dengqu on 2017/9/1.
 */
public class RichEditText extends EditText {
    private final static String TAG = RichEditText.class.getSimpleName();
    public final static int MAX_AT_COUNT = 5;
    // 默认,@好友文本高亮颜色
    private static final int DEFAULT_FOREGROUND_COLOR = Color.parseColor("#FF8C00");
    // 默认,@好友背景高亮颜色
    private static final int DEFAULT_BACKGROUND_COLOR = Color.parseColor("#FFDEAD");

    /**
     * 开发者可设置内容
     */
    private int mForegroundColor = DEFAULT_FOREGROUND_COLOR;// @好友文本高亮颜色
    private int mBackgroundColor = DEFAULT_BACKGROUND_COLOR;// @好友背景高亮颜色
    private ArrayList<XlpsFriend> mXlpsFriends = new ArrayList<XlpsFriend>();// object集合
    private XlpsFriend mXlpsFriend;
    private int mMaxNum = Integer.MAX_VALUE;//最大字数
    private OverLengthListener mOverLengthListener;
    private RefreshEditFinishListener mRefreshEditFinishListener;

    public RichEditText(Context context) {
        this(context, null);
    }

    public RichEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.REditText);
        mBackgroundColor = a
                .getColor(R.styleable.REditText_object_background_color,
                        DEFAULT_BACKGROUND_COLOR);
        mForegroundColor = a
                .getColor(R.styleable.REditText_object_foreground_color,
                        DEFAULT_FOREGROUND_COLOR);
        a.recycle();
        // 初始化设置
        initView();
    }

    /**
     * 监听光标的位置,若光标处于@好友内容中间则移动光标到@好友结束位置
     */
    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        Log.d(TAG, "onSelectionChanged selStart=" + selStart + ",selEnd=" + selEnd);
        super.onSelectionChanged(selStart, selEnd);
        handleOnSelectionChanged(selStart, selEnd);
    }


    /**
     * 处理光标位置发生变化的时候，主要功能是是光标位置不能在@好友之间
     *
     * @param selStart
     * @param selEnd
     */
    private void handleOnSelectionChanged(int selStart, int selEnd) {
        if (mXlpsFriends == null || mXlpsFriends.size() == 0) {
            return;
        }

        int startPosition = 0;
        int endPosition = 0;
        String objectText = "";
        for (int i = 0; i < mXlpsFriends.size(); i++) {
            XlpsFriend xlpsFriend = mXlpsFriends.get(i);
            if (xlpsFriend == null) {
                continue;
            }
            startPosition = xlpsFriend.startPos;// 获取文本开始下标
            endPosition = xlpsFriend.endPos;
            if (selStart == selEnd) {
                if (startPosition >= 0 && endPosition >= startPosition && selStart > startPosition && selStart < endPosition) {// 若光标处于@好友内容中间则移动光标到@好友结束位置
                    Log.d(TAG, "onSelectionChanged startPosition=" + startPosition + "，endPosition=" + endPosition + "，selStart=" + selStart);
                    if (!hasFlagClean()) {
                        setSelection(endPosition);
                    }
                }
            } else {
                if (startPosition >= 0 && endPosition >= startPosition) {// 若光标处于@好友内容中间则移动光标到@好友结束位置
                    Log.d(TAG, "onSelectionChanged startPosition=" + startPosition + "，endPosition=" + endPosition + "，selStart=" + selStart);
                    if (!hasFlagClean()) {
                        if (selEnd > startPosition && selEnd < endPosition) {
                            setSelection(selStart, endPosition);
                        }
                        if (selStart > startPosition && selStart < endPosition) {
                            setSelection(startPosition, selEnd);
                        }
                    }
                }
            }
        }
    }

    /**
     * 是否含有标志清除的
     *
     * @return
     */
    private boolean hasFlagClean() {
        if (mXlpsFriends == null || mXlpsFriends.isEmpty()) {
            return false;
        }
        for (XlpsFriend xlpsFriend : mXlpsFriends) {
            if (xlpsFriend.mFlag == XlpsFriend.FLAG_CLEAR) {
                return true;
            }
        }
        return false;
    }

    /**
     * 初始化控件,一些监听
     */
    private void initView() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "initView onClick-------------");
                XlpsFriend xlpsFriend = checkHasDeleteSelect();
                if (xlpsFriend != null) {
                    getText().setSpan(new BackgroundColorSpan(
                                    Color.TRANSPARENT), xlpsFriend.startPos, xlpsFriend.endPos,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    xlpsFriend.mFlag = XlpsFriend.FLAG_NORMAL;
                }

            }
        });
        /**
         * 输入框内容变化监听<br/>
         * 1.当文字内容产生变化的时候实时更新UI
         */
        this.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                Log.d(TAG, "addTextChangedListener beforeTextChanged s=" + s + ",start=" + start + ",count=" + count + ",after=" + after);
                //resetTextChanged(start, count, after);
                handleBeforeTextChanged(start, count, after);

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                Log.d(TAG, "addTextChangedListener onTextChanged s=" + s + ",start=" + start + ",count=" + count + ",before=" + before);
                handleOnTextChanged(start, before, count);

            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "addTextChangedListener afterTextChanged s=" + s);
                // 文字改变刷新UI
                refreshEditTextUI(s.toString());

            }
        });

        /**
         * 监听删除键 <br/>
         * 1.光标在@好友后面,将整个@好友内容删除 <br/>
         * 2.光标在普通文字后面,删除一个字符
         */
        this.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "setOnKeyListener onKey keyCode=" + keyCode + "，event.getAction() == KeyEvent.ACTION_DOWN " + (event.getAction() == KeyEvent.ACTION_DOWN));
                if (keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN) {

                    int selectionStart = getSelectionStart();
                    int selectionEnd = getSelectionEnd();
                    Log.d(TAG, "setOnKeyListener onKey selectionStart=" + selectionStart + ",selectionEnd=" + selectionEnd);
                    XlpsFriend xlpsFriend = checkHasDeleteSelect();
                    if (xlpsFriend != null) {
                        xlpsFriend.mFlag = XlpsFriend.FLAG_CLEAR;
                        setSelection(xlpsFriend.startPos, xlpsFriend.endPos);
                        mXlpsFriends.remove(xlpsFriend);
                        Log.i(TAG, "setOnKeyListener onKey xlpsFriend != null start=" + xlpsFriend.startPos + ",end=" + xlpsFriend.endPos);

                        return false;
                    }
                    int lastPos = 0;
                    Editable editable = getText();
                    // 遍历判断光标的位置
                    for (int i = 0; i < mXlpsFriends.size(); i++) {
                        XlpsFriend objectFriend = mXlpsFriends.get(i);
                        if (objectFriend != null && objectFriend.startPos >= 0 && objectFriend.endPos >= objectFriend.startPos && selectionStart != 0 && selectionStart == mXlpsFriends.get(i).endPos) {
                            // 选中@好友
                            String objectText = objectFriend.mUserName;
                            Log.d(TAG, "setOnKeyListener onKey start=" + lastPos + ",stop=" + (lastPos + objectText.length()));
                            // 设置背景色
                            editable.setSpan(new BackgroundColorSpan(
                                            mBackgroundColor), objectFriend.startPos, objectFriend.endPos,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            objectFriend.mFlag = XlpsFriend.FLAG_MARK;
                            return true;
                        }
                    }
                }
                return false;
            }
        });

    }


    private void handleBeforeTextChanged(int start, int count,
                                         int after) {
        if (count != 0) {
            if (mXlpsFriends == null || mXlpsFriends.isEmpty()) {
                return;
            }
            List<XlpsFriend> deleteFriends = new ArrayList<>();
            for (XlpsFriend xlpsFriend : mXlpsFriends) {
                if (xlpsFriend.startPos >= start && xlpsFriend.endPos <= start + count) {
                    deleteFriends.add(xlpsFriend);
                }
            }
            if (deleteFriends != null && !deleteFriends.isEmpty()) {
                for (XlpsFriend xlpsFriend : deleteFriends) {
                    Log.d(TAG, "handleBeforeTextChanged remove start=" + xlpsFriend.startPos + ",end=" + xlpsFriend.endPos);
                    mXlpsFriends.remove(xlpsFriend);
                }
            }
            for (XlpsFriend xlpsFriend : mXlpsFriends) {
                if (xlpsFriend.startPos > start) {
                    xlpsFriend.startPos = xlpsFriend.startPos - count;
                    xlpsFriend.endPos = xlpsFriend.endPos - count;
                }
            }
        }
    }

    private void handleOnTextChanged(int start, int before, int count) {
        if (count != 0) {
            if (mXlpsFriends == null || mXlpsFriends.isEmpty()) {
                return;
            }
            for (XlpsFriend xlpsFriend : mXlpsFriends) {
                if (xlpsFriend.startPos >= start) {
                    xlpsFriend.startPos = xlpsFriend.startPos + count;
                    xlpsFriend.endPos = xlpsFriend.endPos + count;
                }
            }
        }
    }

    public void addFriend() {
        RichEditText.XlpsFriend xlpsFriend = new RichEditText.XlpsFriend();
        Random random = new Random();
        xlpsFriend.mUserName = "邓区" + random.nextInt(100);
        addFriend(xlpsFriend);
    }

    public void addFriend(XlpsFriend xlpsFriend) {
        if (xlpsFriend == null) {
            return;
        }
        if (checkAboveMaxCount()) {
            Log.i(TAG, "addFriend checkAboveMaxCount = " + checkAboveMaxCount());
            showAblowMaxCountToast();
            return;
        }
        if (isExistFriend(xlpsFriend)) {
            Log.i(TAG, "addFriend isExistFriend = " + isExistFriend(xlpsFriend));
            showIsExistFriendToast();
            return;
        }
        xlpsFriend.mUserName = xlpsFriend.mUserName + " ";
        setAtFriend(xlpsFriend);
    }

    public void showAblowMaxCountToast() {
        Toast.makeText(getContext(), "最多可以同时@" + MAX_AT_COUNT + "人喔", Toast.LENGTH_SHORT);
    }

    public void showIsExistFriendToast() {
        Toast.makeText(getContext(), "您已@过该好友", Toast.LENGTH_SHORT);
    }

    public boolean isExistFriend(XlpsFriend xlpsFriend) {
        if (xlpsFriend == null || mXlpsFriends == null || mXlpsFriends.isEmpty()) {
            return false;
        }
        for (XlpsFriend friend : mXlpsFriends) {
            if (friend.mUserId == xlpsFriend.mUserId) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查@好友个数是否大于最大限制了
     *
     * @return
     */
    public boolean checkAboveMaxCount() {
        if (mXlpsFriends == null || mXlpsFriends.size() < MAX_AT_COUNT) {
            return false;
        }
        return true;
    }

    /**
     * 检查是否含有标志清除的@好友对象
     *
     * @return
     */
    private XlpsFriend checkHasDeleteSelect() {
        if (mXlpsFriends == null || mXlpsFriends.isEmpty()) {
            return null;
        }
        for (XlpsFriend xlpsFriend : mXlpsFriends) {
            if (xlpsFriend.mFlag == XlpsFriend.FLAG_MARK) {
                return xlpsFriend;
            }
        }
        return null;
    }

    /**
     * EditText内容修改之后刷新UI
     *
     * @param content
     */
    private void refreshEditTextUI(String content) {

        /**
         * 内容变化时操作<br/>
         * 1.查找匹配所有@好友内容 <br/>
         * 2.设置@好友内容特殊颜色
         */

        if (TextUtils.isEmpty(content)) {
            mXlpsFriends.clear();
            return;
        }

        if (mXlpsFriend != null) {
            mXlpsFriends.add(mXlpsFriend.copy());
            mXlpsFriend = null;
        }


        if (mXlpsFriends.size() == 0)
            return;

        /**
         * 重新设置span
         */
        Editable editable = getText();
        int findPosition = 0;
        for (int i = 0; i < mXlpsFriends.size(); i++) {
            final XlpsFriend object = mXlpsFriends.get(i);
            Log.i(TAG, "refreshEditTextUI i=" + i + ",findPosition=" + findPosition);
            if (object != null && object.startPos >= 0 && object.endPos >= object.startPos && object.endPos <= editable.length()) {
                ForegroundColorSpan colorSpan = new ForegroundColorSpan(
                        mForegroundColor);
                editable.setSpan(colorSpan, object.startPos, object.endPos,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        XlpsFriend xlpsFriend = checkHasDeleteSelect();
        if (xlpsFriend != null) {
            mXlpsFriends.remove(xlpsFriend);
            getText().delete(xlpsFriend.startPos, xlpsFriend.endPos);
        }
        if (mRefreshEditFinishListener != null) {
            mRefreshEditFinishListener.onRefreshEditFinish();
        }
    }

    /**
     * 插入/设置@好友
     *
     * @param object@好友对象
     */
    public void setAtFriend(XlpsFriend object) {

        if (object == null)
            return;

        String objectRule = object.mRule;
        String objectText = object.mUserName;
        if (TextUtils.isEmpty(objectText) || TextUtils.isEmpty(objectRule))
            return;

        // 拼接字符# %s #,并保存
        objectText = objectRule + objectText;
        object.mUserName = objectText;
        /**
         * 添加@好友<br/>
         * 1.将@好友内容添加到数据集合中<br/>
         * 2.将@好友内容添加到EditText中展示
         */

        /**
         * 1.添加@好友内容到数据集合
         */
        object.startPos = getSelectionStart();
        object.endPos = object.startPos + objectText.length();
        int curLength = getText().length();
        int objectTextLength = objectText.length();
        Log.i(TAG, "setAtFriend curLength =" + curLength);
        Log.i(TAG, "setAtFriend objectTextLength =" + objectTextLength);
        if (curLength + objectTextLength > getMaxNum()) {
            if (mOverLengthListener != null) {
                mOverLengthListener.onOverLength();
            }
            return;
        }
        mXlpsFriend = object;


        /**
         * 2.将@好友内容添加到EditText中展示
         */
        int selectionStart = getSelectionStart();// 光标位置
        Editable editable = getText();// 原先内容

        if (selectionStart >= 0) {
            editable.insert(selectionStart, objectText);// 在光标位置插入内容
            //editable.insert(getSelectionStart(), " ");// @好友后面插入空格,至关重要
            setSelection(getSelectionStart());// 移动光标到添加的内容后面
        }

    }


    /**
     * 获取mXlpsFriends列表数据
     */
    public ArrayList<XlpsFriend> getXlpsFriends() {
        return mXlpsFriends;
    }


    public String getAtFriendText() {
        String text = getText().toString();
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        if (mXlpsFriends == null || mXlpsFriends.isEmpty()) {
            return text;
        }
        Collections.sort(mXlpsFriends, new Comparator<XlpsFriend>() {
            @Override
            public int compare(XlpsFriend lhs, XlpsFriend rhs) {
                return lhs.startPos - rhs.startPos;
            }
        });
        Log.d(TAG, "getAtFriendText text=" + text);
        StringBuilder stringBuilder = new StringBuilder();
        int prePos = 0;
        for (XlpsFriend xlpsFriend : mXlpsFriends) {
            Log.d(TAG, "getAtFriendText xlpsFriend start=" + xlpsFriend.startPos + ",end=" + xlpsFriend.endPos + ",prePos=" + prePos);
            stringBuilder.append(text.substring(prePos, xlpsFriend.startPos));
            stringBuilder.append("@" + xlpsFriend.mUserId + " ");
            prePos = xlpsFriend.endPos;
        }
        stringBuilder.append(text.substring(prePos, text.length()));
        Log.i(TAG, "getAtFriendText stringBuilder=" + stringBuilder.toString());
        return stringBuilder.toString();
    }

    public int getMaxNum() {
        return mMaxNum;
    }

    public void setMaxNum(int mMaxNum) {
        this.mMaxNum = mMaxNum;
    }

    public void setOverLengthListener(OverLengthListener overLengthListener) {
        mOverLengthListener = overLengthListener;
    }

    public void setXlpsFriends(ArrayList<XlpsFriend> xlpsFriends) {
        if (xlpsFriends == null || xlpsFriends.isEmpty()) {
            return;
        }
        if (mXlpsFriends != null) {
            mXlpsFriends.addAll(xlpsFriends);
            refreshEditTextUI(getText().toString());
        }
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        switch (id) {
            case android.R.id.copy:
                resetFriendBg();
                break;
        }
        boolean result = super.onTextContextMenuItem(id);
        refreshEditTextUI(getText().toString());
        return result;

    }

    /**
     * 更新edittext的文本颜色，不然复制下来的文本也会含有颜色的
     */
    private void resetFriendBg() {
        if (mXlpsFriends == null || mXlpsFriends.isEmpty()) {
            return;
        }
        for (XlpsFriend xlpsFriend : mXlpsFriends) {
            ForegroundColorSpan colorSpan = new ForegroundColorSpan(
                    Color.WHITE);
            getText().setSpan(colorSpan, xlpsFriend.startPos, xlpsFriend.endPos,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
    }

    public void setRefreshEditFinishListener(RefreshEditFinishListener refreshEditFinishListener) {
        mRefreshEditFinishListener = refreshEditFinishListener;
    }


    public static class XlpsFriend implements Serializable {
        public final static int FLAG_NORMAL = 1000;//正常状态
        public final static int FLAG_MARK = 1001;//被标记,准备清除
        public final static int FLAG_CLEAR = 1002;//标记清除
        public int mFlag = FLAG_NORMAL;
        public String mRule = "@";// 匹配规则
        public long mUserId;//用户id
        public String mUserName;// 高亮文本
        public int startPos;
        public int endPos;

        public XlpsFriend copy() {
            XlpsFriend xlpsFriend = new XlpsFriend();
            xlpsFriend.mFlag = mFlag;
            xlpsFriend.mRule = mRule;
            xlpsFriend.mUserId = mUserId;
            xlpsFriend.mUserName = mUserName;
            xlpsFriend.startPos = startPos;
            xlpsFriend.endPos = endPos;
            return xlpsFriend;
        }

        public JSONObject getJsonObject() {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject();
                jsonObject.put("flag", mFlag);
                jsonObject.put("rule", mRule);
                jsonObject.put("user_id", mUserId);
                jsonObject.put("user_name", mUserName);
                jsonObject.put("start_pos", startPos);
                jsonObject.put("end_pos", endPos);
            } catch (JSONException e) {
                jsonObject = null;
            }
            return jsonObject;
        }

        public static XlpsFriend parseJSONObject(JSONObject jsonObject) {
            if (jsonObject != null) {
                XlpsFriend xlpsFriend = new XlpsFriend();
                xlpsFriend.mFlag = jsonObject.optInt("flag");
                xlpsFriend.mRule = jsonObject.optString("rule");
                xlpsFriend.mUserId = jsonObject.optLong("user_id");
                xlpsFriend.mUserName = jsonObject.optString("user_name");
                xlpsFriend.startPos = jsonObject.optInt("start_pos");
                xlpsFriend.endPos = jsonObject.optInt("end_pos");
                return xlpsFriend;
            }
            return null;
        }
    }

    public interface RefreshEditFinishListener {
        public void onRefreshEditFinish();
    }


    public class EditLengthFilter implements InputFilter {
        private final int mMax;
        private OverLengthListener mListener;

        public EditLengthFilter(int max, OverLengthListener listener) {
            mMax = max;
            mListener = listener;
        }

        public EditLengthFilter(int max) {
            mMax = max;
//        mTip = ;
        }

        public void setListener(OverLengthListener listener) {
            mListener = listener;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                                   int dstart, int dend) {
            int keep = mMax - (dest.length() - (dend - dstart));
            if (keep <= 0) {
                if (mListener != null) {
                    mListener.onOverLength();
                }
//
                return "";
            } else if (keep >= end - start) {
                return null; // keep original
            } else {
                keep += start;
                if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                    --keep;
                    if (keep == start) {
                        return "";
                    }
                }
                return source.subSequence(start, keep);
            }
        }


    }

    public interface OverLengthListener {
        void onOverLength();
    }
}

