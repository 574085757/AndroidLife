package me.ele.uetool;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import me.ele.uetool.base.DimenUtil;
import me.ele.uetool.base.Element;

import static me.ele.uetool.base.DimenUtil.dip2px;
import static me.ele.uetool.base.DimenUtil.getScreenHeight;
import static me.ele.uetool.base.DimenUtil.getScreenWidth;
import static me.ele.uetool.base.DimenUtil.px2dip;
import static me.ele.uetool.base.DimenUtil.sp2px;

@SuppressWarnings("DanglingJavadoc")
public class CollectViewsLayout extends View {

    private final int halfEndPointWidth = dip2px(2.5f);
    private final int textBgFillingSpace = dip2px(2);
    private final int textLineDistance = dip2px(5);
    protected final int screenWidth = getScreenWidth();
    protected final int screenHeight = getScreenHeight();

    protected List<Element> elements = new ArrayList<>();
    protected Element childElement, parentElement;
    protected Paint textPaint = new Paint() {
        {
            setAntiAlias(true);
            setTextSize(sp2px(10));
            setColor(Color.RED);
            setStrokeWidth(dip2px(1));
        }
    };

    private Paint textBgPaint = new Paint() {
        {
            setAntiAlias(true);
            setColor(Color.WHITE);
            setStrokeJoin(Join.ROUND);
        }
    };

    protected Paint dashLinePaint = new Paint() {
        {
            setAntiAlias(true);
            setColor(0x90FF0000);
            setStyle(Style.STROKE);
            setPathEffect(new DashPathEffect(new float[] { dip2px(4), dip2px(8) }, 0));
        }
    };


    public CollectViewsLayout(Context context) {
        super(context);
    }


    public CollectViewsLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    public CollectViewsLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    /**
     * 通过反射 WindowManagerImpl，拿到一堆 Activity 对应的 DecorView
     * 然后逐个遍历这些 DecorView，拿到一颗颗 View tree。保存下来，收集信息
     *
     * API > 4.1:
     * - 反射「WindowManagerImpl # WindowManagerGlobal mGlobal」
     * - API > 4.1 && API <= 6.0
     * -   - 反射「WindowManagerImpl # WindowManagerGlobal mGlobal」
     * -   - 反射「WindowManagerGlobal # ArrayList<View> mViews」
     * -   -   - API >= 4.4 && API <= 6.0
     * -   -   -   - 「WindowManagerGlobal # ArrayList<View> mViews」
     * -   -   - API > 4.1 && API < 4.3
     * -   -   -   - 「WindowManagerGlobal # View[] mViews」
     * -   -   - 这里的每个 View，对应着一个个 DecorView，就是每个 Activity 的最顶层布局
     * -   -   - 先拿到每个 DecorView
     * -   -   - 然后 DecorView 开始，递归遍历整个 View tree，拿到一颗颗 View tree
     * - API > 6.0
     * -   - 反射「WindowManagerGlobal # ArrayList<ViewRootImpl> mRoots」
     * -   - 获取每个 ViewRootImpl
     * -   - 反射获取
     * -   - 「ViewRootImpl # WindowManager.LayoutParams mWindowAttributes」
     * -   - 「ViewRootImpl # View mView」这个其实是「DecorView」
     * -   - 先拿到每个 DecorView
     * -   - 然后 DecorView 开始，递归遍历整个 View tree，拿到一颗颗 View tree
     *
     * API <= 4.1:
     * - 反射
     * - 「WindowManagerImpl # CompatModeWrapper # WindowManagerImpl mWindowManager」
     * - 「WindowManagerImpl # View[] mViews」
     * -  第一则反射 先拿到 WindowManagerImpl 实例
     * -  然后再拿到「WindowManagerImpl # View[] mViews」
     * -  这里面每个 View 都是 DecorView
     * -  然后 DecorView 开始，递归遍历整个 View tree，拿到一颗颗 View tree
     */
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        try {
            Activity targetActivity = UETool.getInstance().getTargetActivity();
            WindowManager windowManager = targetActivity.getWindowManager();

            /**
             * API > 4.1
             */
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {

                /**
                 * 反射「WindowManagerImpl # WindowManagerGlobal mGlobal」
                 */
                Field mGlobalField = Class.forName("android.view.WindowManagerImpl")
                    .getDeclaredField("mGlobal");
                mGlobalField.setAccessible(true);

                /**
                 * API <= 6.0
                 */
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                    /**
                     * 反射「WindowManagerGlobal # ArrayList<View> mViews」
                     */
                    Field mViewsField = Class.forName("android.view.WindowManagerGlobal")
                        .getDeclaredField("mViews");
                    mViewsField.setAccessible(true);
                    List<View> views;

                    /**
                     * API >= 4.4
                     * 「WindowManagerGlobal # ArrayList<View> mViews」
                     *
                     * API < 4.3
                     * 「WindowManagerGlobal # View[] mViews」
                     */
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        views = (List<View>) mViewsField.get(mGlobalField.get(windowManager));
                    } else {
                        views = Arrays.asList(
                            (View[]) mViewsField.get(mGlobalField.get(windowManager)));
                    }

                    /**
                     * 这里的每个 View，对应着一个个 DecorView，就是每个 Activity 的最顶层布局
                     * 先拿到每个 DecorView
                     * 然后 DecorView 开始，递归遍历整个 View tree，拿到一颗颗 View tree
                     */
                    for (int i = views.size() - 1; i >= 0; i--) {
                        View targetView = getTargetDecorView(targetActivity, views.get(i));
                        if (targetView != null) {
                            traverse(targetView);
                            break;
                        }
                    }
                } else {
                    /**
                     * API > 6.0
                     */

                    /**
                     * 反射「WindowManagerGlobal # ArrayList<ViewRootImpl> mRoots」
                     */
                    Field mRootsField = Class.forName("android.view.WindowManagerGlobal")
                        .getDeclaredField("mRoots");
                    mRootsField.setAccessible(true);
                    List viewRootImpls;
                    viewRootImpls = (List) mRootsField.get(mGlobalField.get(windowManager));
                    /**
                     * 获取每个 ViewRootImpl
                     * 反射获取
                     * 「ViewRootImpl # WindowManager.LayoutParams mWindowAttributes」
                     * 「ViewRootImpl # View mView」这个其实是「DecorView」
                     *
                     * 先拿到每个 DecorView
                     * 然后 DecorView 开始，递归遍历整个 View tree，拿到一颗颗 View tree
                     */
                    for (int i = viewRootImpls.size() - 1; i >= 0; i--) {
                        Class clazz = Class.forName("android.view.ViewRootImpl");
                        Object object = viewRootImpls.get(i);
                        Field mWindowAttributesField = clazz.getDeclaredField("mWindowAttributes");
                        mWindowAttributesField.setAccessible(true);
                        Field mViewField = clazz.getDeclaredField("mView");
                        mViewField.setAccessible(true);
                        View decorView = (View) mViewField.get(object);
                        WindowManager.LayoutParams layoutParams
                            = (WindowManager.LayoutParams) mWindowAttributesField.get(object);
                        if (layoutParams.getTitle()
                            .toString()
                            .contains(targetActivity.getClass().getName())
                            || getTargetDecorView(targetActivity, decorView) != null) {
                            traverse(decorView);
                            break;
                        }
                    }
                }
            } else {
                /**
                 * API <= 4.1
                 */

                // http://androidxref.com/4.1.1/xref/frameworks/base/core/java/android/view/WindowManagerImpl.java

                /**
                 * 反射
                 * 「WindowManagerImpl # CompatModeWrapper # WindowManagerImpl mWindowManager」
                 * 「WindowManagerImpl # View[] mViews」
                 *
                 * 第一则反射 先拿到 WindowManagerImpl 实例
                 * 然后再拿到「WindowManagerImpl # View[] mViews」
                 * 这里面每个 View 都是 DecorView
                 *
                 * 然后 DecorView 开始，递归遍历整个 View tree，拿到一颗颗 View tree
                 */
                Field mWindowManagerField = Class.forName(
                    "android.view.WindowManagerImpl$CompatModeWrapper")
                    .getDeclaredField("mWindowManager");
                mWindowManagerField.setAccessible(true);
                Field mViewsField = Class.forName("android.view.WindowManagerImpl")
                    .getDeclaredField("mViews");
                mViewsField.setAccessible(true);
                List<View> views = Arrays.asList(
                    (View[]) mViewsField.get(mWindowManagerField.get(windowManager)));
                for (int i = views.size() - 1; i >= 0; i--) {
                    View targetView = getTargetDecorView(targetActivity, views.get(i));
                    if (targetView != null) {
                        traverse(targetView);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        elements.clear();
        childElement = null;
        parentElement = null;
    }


    /**
     * 从 Activity 的 DecorView 开始，递归遍历整个 View tree
     * 这种递归方式不好... 可以用「树的深度算法」「前序遍历非递归法」去优化...
     *
     * @param view view
     */
    private void traverse(View view) {
        if (UETool.getInstance().getFilterClasses().contains(view.getClass().getName())) return;
        if (view.getAlpha() == 0 || view.getVisibility() != View.VISIBLE) return;
        if (getResources().getString(R.string.uet_disable).equals(view.getTag())) return;
        if (view instanceof ViewGroup) {
            elements.add(0, new Element(view));
            ViewGroup parent = (ViewGroup) view;
            for (int i = 0; i < parent.getChildCount(); i++) {
                traverse(parent.getChildAt(i));
            }
        } else {
            elements.add(new Element(view));
        }
    }


    /**
     * 拿到 Activity 的 DecorView
     *
     * @param targetActivity targetActivity
     * @param decorView decorView
     * @return View
     */
    private View getTargetDecorView(Activity targetActivity, View decorView) {
        View targetView = null;
        Context context = decorView.getContext();
        if (context == targetActivity) {
            targetView = decorView;
        } else {
            while (context instanceof ContextThemeWrapper) {
                Context baseContext = ((ContextThemeWrapper) context).getBaseContext();
                if (baseContext == targetActivity) {
                    targetView = decorView;
                    break;
                }
                context = baseContext;
            }
        }
        return targetView;
    }


    /**
     * 根据 x y
     * 获取对应的 Element
     * 「当然要在之前遍历各个 Activity view tree 的时候缓存下来的 Element 数组里」
     *
     * @param x x
     * @param y y
     * @return Element
     */
    protected Element getTargetElement(float x, float y) {
        Element target = null;
        for (int i = elements.size() - 1; i >= 0; i--) {
            final Element element = elements.get(i);
            if (element.getRect().contains((int) x, (int) y)) {
                if (isParentNotVisible(element.getParentElement())) {
                    continue;
                }
                if (element != childElement) {
                    childElement = element;
                    parentElement = element;
                } else if (parentElement != null) {
                    parentElement = parentElement.getParentElement();
                }
                target = parentElement == null ? element : parentElement;
                break;
            }
        }
        if (target == null) {
            Toast.makeText(getContext(),
                getResources().getString(R.string.uet_target_element_not_found, x, y),
                Toast.LENGTH_SHORT).show();
        }
        return target;
    }


    /**
     * 判断这个 Element 是否在屏幕上
     *
     * 不是的话，递归往上走找 父 Element 继续判
     * 要么，找到屏幕上的父 Element。然后，返回 true
     * 要么找到头，都没有返回 false
     *
     * 是的话，true
     *
     * @param parent parent
     * @return boolean
     */
    private boolean isParentNotVisible(Element parent) {
        if (parent == null) {
            return false;
        }
        if (parent.getRect().left >= DimenUtil.getScreenWidth()
            || parent.getRect().top >= DimenUtil.getScreenHeight()) {
            return true;
        } else {
            return isParentNotVisible(parent.getParentElement());
        }
    }


    /**
     * 根据 x y
     * 获取对应的 Element
     * 返回一个数组
     * 「当然要在之前遍历各个 Activity view tree 的时候缓存下来的 Element 数组里」
     *
     * @param x x
     * @param y y
     * @return List<Element>
     */
    protected List<Element> getTargetElements(float x, float y) {
        List<Element> validList = new ArrayList<>();
        for (int i = elements.size() - 1; i >= 0; i--) {
            final Element element = elements.get(i);
            if (element.getRect().contains((int) x, (int) y)) {
                validList.add(element);
            }
        }
        return validList;
    }


    /**
     * 绘制 文本
     *
     * @param canvas canvas
     * @param text text
     * @param x x
     * @param y y
     */
    protected void drawText(Canvas canvas, String text, float x, float y) {
        float left = x - textBgFillingSpace;
        float top = y - getTextHeight(text);
        float right = x + getTextWidth(text) + textBgFillingSpace;
        float bottom = y + textBgFillingSpace;
        // ensure text in screen bound
        if (left < 0) {
            right -= left;
            left = 0;
        }
        if (top < 0) {
            bottom -= top;
            top = 0;
        }
        if (bottom > screenHeight) {
            float diff = top - bottom;
            bottom = screenHeight;
            top = bottom + diff;
        }
        if (right > screenWidth) {
            float diff = left - right;
            right = screenWidth;
            left = right + diff;
        }
        canvas.drawRect(left, top, right, bottom, textBgPaint);
        canvas.drawText(text, left + textBgFillingSpace, bottom - textBgFillingSpace, textPaint);
    }


    /**
     * 绘制 行 和 点
     *
     * @param canvas canvas
     * @param startX startX
     * @param startY startY
     * @param endX endX
     * @param endY endY
     */
    private void drawLineWithEndPoint(Canvas canvas, int startX, int startY, int endX, int endY) {
        canvas.drawLine(startX, startY, endX, endY, textPaint);
        if (startX == endX) {
            canvas.drawLine(startX - halfEndPointWidth, startY, endX + halfEndPointWidth, startY,
                textPaint);
            canvas.drawLine(startX - halfEndPointWidth, endY, endX + halfEndPointWidth, endY,
                textPaint);
        } else if (startY == endY) {
            canvas.drawLine(startX, startY - halfEndPointWidth, startX, endY + halfEndPointWidth,
                textPaint);
            canvas.drawLine(endX, startY - halfEndPointWidth, endX, endY + halfEndPointWidth,
                textPaint);
        }
    }


    protected void drawLineWithText(Canvas canvas, int startX, int startY, int endX, int endY) {
        drawLineWithText(canvas, startX, startY, endX, endY, 0);
    }


    /**
     * 绘制 行 和 文本
     *
     * @param canvas canvas
     * @param startX startX
     * @param startY startY
     * @param endX endX
     * @param endY endY
     * @param endPointSpace endPointSpace
     */
    protected void drawLineWithText(Canvas canvas, int startX, int startY, int endX, int endY, int endPointSpace) {

        if (startX == endX && startY == endY) {
            return;
        }

        if (startX > endX) {
            int tempX = startX;
            startX = endX;
            endX = tempX;
        }
        if (startY > endY) {
            int tempY = startY;
            startY = endY;
            endY = tempY;
        }

        if (startX == endX) {
            drawLineWithEndPoint(canvas, startX, startY + endPointSpace, endX,
                endY - endPointSpace);
            String text = px2dip(endY - startY, true);
            drawText(canvas, text, startX + textLineDistance,
                startY + (endY - startY) / 2 + getTextHeight(text) / 2);
        } else if (startY == endY) {
            drawLineWithEndPoint(canvas, startX + endPointSpace, startY, endX - endPointSpace,
                endY);
            String text = px2dip(endX - startX, true);
            drawText(canvas, text, startX + (endX - startX) / 2 - getTextWidth(text) / 2,
                startY - textLineDistance);
        }
    }


    /**
     * 根据文字
     * 拿文本高度「得有画笔才行」
     *
     * @param text text
     * @return float
     */
    protected float getTextHeight(String text) {
        Rect rect = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), rect);
        return rect.height();
    }


    /**
     * 根据文字
     * 拿文本宽度「得有画笔才行」
     *
     * @param text text
     * @return float
     */
    protected float getTextWidth(String text) {
        return textPaint.measureText(text);
    }
}
