package me.ele.uetool;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static me.ele.uetool.TransparentActivity.Type.TYPE_UNKNOWN;

/**
 * 功能菜单
 */
public class UETMenu extends LinearLayout {

    private View vMenu;
    private ViewGroup vSubMenuContainer;
    private ValueAnimator animator;
    private Interpolator defaultInterpolator = new AccelerateDecelerateInterpolator();
    private List<UETSubMenu.SubMenu> subMenus = new ArrayList<>();

    private WindowManager windowManager;
    private WindowManager.LayoutParams params = new WindowManager.LayoutParams();
    private int touchSlop;
    private int y;


    /**
     * 「1」添加子菜单
     * 「2」设置动画
     * 「3」添加「可拖动」功能的 TouchListener「更新 window params 参数，实现移动」
     *
     * 这个 TouchListener 因为覆写了 onTouch 事件
     * 可能会妨碍到 onClick 事件。所以在，判断不是移动 event 的时候
     * 反射
     * 「View # ListenerInfo mListenerInfo」拿到 ListenerInfo mListenerInfo
     * 再反射
     * 「ListenerInfo # OnClickListener mOnClickListener」拿到 OnClickListener mOnClickListener
     * 然后手动调用
     *
     * 就是，当且仅当，视为不是移动时，由于设置了 onTouchListener，需要手动调用
     * OnClickListener mOnClickListener 进而回到 onClick 的流程里
     * 这里的 onClick 就是展开或者收起菜单，并且进行动画
     *
     * 换句话说，这里不处理好 TouchListener 的话，就不会展开或者收起
     *
     * @param context context
     * @param y y
     */
    public UETMenu(final Context context, int y) {
        super(context);
        inflate(context, R.layout.uet_menu_layout, this);
        setGravity(Gravity.CENTER_VERTICAL);

        this.y = y;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        vMenu = findViewById(R.id.menu);
        vSubMenuContainer = findViewById(R.id.sub_menu_container);
        Resources resources = context.getResources();
        subMenus.add(new UETSubMenu.SubMenu(resources.getString(R.string.uet_catch_view),
            R.drawable.uet_edit_attr, new OnClickListener() {
            @Override
            public void onClick(View v) {
                open(TransparentActivity.Type.TYPE_EDIT_ATTR);
            }
        }));
        subMenus.add(new UETSubMenu.SubMenu(resources.getString(R.string.uet_relative_location),
            R.drawable.uet_relative_position,
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(TransparentActivity.Type.TYPE_RELATIVE_POSITION);
                }
            }));
        subMenus.add(new UETSubMenu.SubMenu(resources.getString(R.string.uet_grid),
            R.drawable.uet_show_gridding,
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(TransparentActivity.Type.TYPE_SHOW_GRIDDING);
                }
            }));

        for (UETSubMenu.SubMenu subMenu : subMenus) {
            UETSubMenu uetSubMenu = new UETSubMenu(getContext());
            uetSubMenu.update(subMenu);
            vSubMenuContainer.addView(uetSubMenu);
        }

        vMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startAnim();
            }
        });

        vMenu.setOnTouchListener(new OnTouchListener() {
            private float downX, downY;
            private float lastY;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        lastY = downY;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        params.y += event.getRawY() - lastY;
                        params.y = Math.max(0, params.y);
                        windowManager.updateViewLayout(UETMenu.this, params);
                        lastY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - downX) < touchSlop &&
                            Math.abs(event.getRawY() - downY) < touchSlop) {
                            try {
                                Field field = View.class.getDeclaredField("mListenerInfo");
                                field.setAccessible(true);
                                Object object = field.get(vMenu);
                                field = object.getClass().getDeclaredField("mOnClickListener");
                                field.setAccessible(true);
                                object = field.get(object);
                                if (object != null && object instanceof OnClickListener) {
                                    ((OnClickListener) object).onClick(vMenu);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                }
                return true;
            }
        });
    }


    /**
     * 菜单展开 或者 收起动画
     */
    private void startAnim() {
        ensureAnim();
        final boolean isOpen = vSubMenuContainer.getTranslationX() <= -vSubMenuContainer.getWidth();
        animator.setInterpolator(
            isOpen ? defaultInterpolator : new ReverseInterpolator(defaultInterpolator));
        animator.removeAllListeners();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                vSubMenuContainer.setVisibility(VISIBLE);
            }


            @Override
            public void onAnimationEnd(Animator animation) {
                if (!isOpen) {
                    vSubMenuContainer.setVisibility(GONE);
                }
            }
        });
        animator.start();
    }


    private void ensureAnim() {
        if (animator == null) {
            animator = ValueAnimator.ofInt(-vSubMenuContainer.getWidth(), 0);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    vSubMenuContainer.setTranslationX((int) animation.getAnimatedValue());
                }
            });
            animator.setDuration(400);
        }
    }


    private void open() {
        open(TYPE_UNKNOWN);
    }


    /**
     * 打开「捕捉控件」「相对位置」「网格栅栏」对应的 TransparentActivity
     *
     * @param type type
     */
    private void open(@TransparentActivity.Type int type) {
        Activity currentTopActivity = Util.getCurrentActivity();
        if (currentTopActivity == null) {
            return;
        } else if (currentTopActivity.getClass() == TransparentActivity.class) {
            currentTopActivity.finish();
            return;
        }
        Intent intent = new Intent(currentTopActivity, TransparentActivity.class);
        intent.putExtra(TransparentActivity.EXTRA_TYPE, type);
        currentTopActivity.startActivity(intent);
        currentTopActivity.overridePendingTransition(0, 0);
        UETool.getInstance().setTargetActivity(currentTopActivity);
    }


    public void show() {
        try {
            windowManager.addView(this, getWindowLayoutParams());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public int dismiss() {
        try {
            windowManager.removeView(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return params.y;
    }


    /**
     * 获取 window layout params 参数
     * 做了一下兼容
     *
     * FIRST_SYSTEM_WINDOW = 2000
     * TYPE_SYSTEM_ALERT = FIRST_SYSTEM_WINDOW + 3
     * TYPE_APPLICATION_OVERLAY = FIRST_SYSTEM_WINDOW + 38
     *
     * 意味着 O 以后，拿的 TYPE_APPLICATION_OVERLAY 在更上一层
     * 系统发展过程，对窗口权限的收紧
     *
     * @return WindowManager.LayoutParams
     */
    private WindowManager.LayoutParams getWindowLayoutParams() {
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 10;
        params.y = y;
        return params;
    }


    private static class ReverseInterpolator implements TimeInterpolator {

        private TimeInterpolator mWrappedInterpolator;


        ReverseInterpolator(TimeInterpolator interpolator) {
            mWrappedInterpolator = interpolator;
        }


        @Override
        public float getInterpolation(float input) {
            return mWrappedInterpolator.getInterpolation(Math.abs(input - 1f));
        }
    }
}
