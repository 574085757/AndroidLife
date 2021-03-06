package me.ele.uetool.fresco;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.drawable.FadeDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.view.DraweeView;
import com.facebook.drawee.view.GenericDraweeView;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import me.ele.uetool.base.Element;
import me.ele.uetool.base.IAttrs;
import me.ele.uetool.base.item.BitmapItem;
import me.ele.uetool.base.item.Item;
import me.ele.uetool.base.item.TextItem;
import me.ele.uetool.base.item.TitleItem;

import static me.ele.uetool.base.DimenUtil.px2dip;

public class UETFresco implements IAttrs {

    @Override
    public List<Item> getAttrs(Element element) {
        List<Item> items = new ArrayList<>();

        View view = element.getView();

        if (view instanceof DraweeView) {
            items.add(new TitleItem("DraweeView"));
            items.add(new TextItem("CornerRadius", getCornerRadius((DraweeView) view)));
            items.add(new TextItem("ImageURI", getImageURI((DraweeView) view), true));
            items.add(new TextItem("ActualScaleType", getScaleType((DraweeView) view), true));
            items.add(new TextItem("IsSupportAnimation", isSupportAnimation((DraweeView) view)));
            items.add(new BitmapItem("PlaceHolderImage", getPlaceHolderBitmap((DraweeView) view)));
            items.add(new TextItem("FadeDuration", getFadeDuration((DraweeView) view)));
        }
        return items;
    }


    /**
     * 圆角半径「dp」
     *
     * 通过「Fresco」的 ImageView「DraweeView」
     * 先拿到 层级「GenericDraweeHierarchy」
     * 再获取圆角的 半径「dp」
     *
     * @param draweeView draweeView
     * @return String
     */
    private String getCornerRadius(DraweeView draweeView) {
        GenericDraweeHierarchy hierarchy = getGenericDraweeHierarchy(draweeView);
        if (hierarchy != null) {
            RoundingParams params = hierarchy.getRoundingParams();
            if (params != null) {
                float[] cornersRadii = params.getCornersRadii();
                float firstRadii = cornersRadii[0];
                for (int i = 1; i < 8; i++) {
                    if (firstRadii != cornersRadii[i]) {
                        return null;
                    }
                }
                return px2dip(firstRadii, true);
            }
        }
        return null;
    }


    /**
     * 「scaleType」
     *
     * 通过「Fresco」的 ImageView「DraweeView」
     * 先拿到 层级「GenericDraweeHierarchy」
     * 再获取 scaleType
     *
     * @param draweeView draweeView
     * @return String
     */
    private String getScaleType(DraweeView draweeView) {
        GenericDraweeHierarchy hierarchy = getGenericDraweeHierarchy(draweeView);
        if (hierarchy != null) {
            return hierarchy.getActualImageScaleType().toString().toUpperCase();
        }
        return null;
    }


    /**
     * 「uri」
     *
     * 通过「Fresco」的 ImageView「DraweeView」
     * 先拿到 ImageView「DraweeView」 的 builder「PipelineDraweeControllerBuilder」
     * 再获取 uri
     *
     * @param draweeView draweeView
     * @return String
     */
    private String getImageURI(DraweeView draweeView) {
        PipelineDraweeControllerBuilder builder = getFrescoControllerBuilder(draweeView);
        if (builder != null) {
            return builder.getImageRequest().getSourceUri().toString();
        }
        return "";
    }


    /**
     * 「gif」判断
     *
     * 通过「Fresco」的 ImageView「DraweeView」
     * 先拿到 ImageView「DraweeView」 的 builder「PipelineDraweeControllerBuilder」
     * 再获取 是否支持动画「gif」
     *
     * @param draweeView draweeView
     * @return String
     */
    private String isSupportAnimation(DraweeView draweeView) {
        PipelineDraweeControllerBuilder builder = getFrescoControllerBuilder(draweeView);
        if (builder != null) {
            return String.valueOf(builder.getAutoPlayAnimations()).toUpperCase();
        }
        return "";
    }


    /**
     *「占位图」
     *
     * 「1」反射拿「GenericDraweeHierarchy # FadeDrawable mFadeDrawable」
     * 「2」反射拿「FadeDrawable # Drawable[] mLayers」
     * 「3」因为「GenericDraweeHierarchy # int PLACEHOLDER_IMAGE_INDEX = 1」
     * 「4」取 「FadeDrawable # Drawable[] mLayers」中的 1
     *
     * @param draweeView draweeView
     * @return Bitmap
     */
    private Bitmap getPlaceHolderBitmap(DraweeView draweeView) {
        GenericDraweeHierarchy hierarchy = getGenericDraweeHierarchy(draweeView);
        if (hierarchy != null && hierarchy.hasPlaceholderImage()) {
            try {
                Field mFadeDrawableField = hierarchy.getClass().getDeclaredField("mFadeDrawable");
                mFadeDrawableField.setAccessible(true);
                FadeDrawable fadeDrawable = (FadeDrawable) mFadeDrawableField.get(hierarchy);
                Field mLayersField = fadeDrawable.getClass().getDeclaredField("mLayers");
                mLayersField.setAccessible(true);
                Drawable[] layers = (Drawable[]) mLayersField.get(fadeDrawable);
                // PLACEHOLDER_IMAGE_INDEX == 1
                Drawable drawable = layers[1];
                return getFrescoDrawableBitmap(drawable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    /**
     * 「渐进时间」
     *
     * 通过「Fresco」的 ImageView「DraweeView」
     * 先拿到 层级「GenericDraweeHierarchy」
     * 再获取 渐进时间
     *
     * @param draweeView draweeView
     * @return String
     */
    private String getFadeDuration(DraweeView draweeView) {
        int duration = 0;
        GenericDraweeHierarchy hierarchy = getGenericDraweeHierarchy(draweeView);
        if (hierarchy != null) {
            duration = hierarchy.getFadeDuration();
        }
        return duration + "ms";
    }


    /**
     * 「Fresco 层级」
     *
     * 通过「Fresco」的 ImageView「DraweeView」
     * 拿到 层级「GenericDraweeHierarchy」
     *
     * @param draweeView draweeView
     * @return GenericDraweeHierarchy
     */
    private GenericDraweeHierarchy getGenericDraweeHierarchy(DraweeView draweeView) {
        if (draweeView instanceof GenericDraweeView) {
            return ((GenericDraweeView) draweeView).getHierarchy();
        }
        return null;
    }


    /**
     * 通过「Fresco」的 ImageView「DraweeView」
     * 获取到这个 ImageView「DraweeView」对应的 builder「Builder 模式中的 builder，用于存储数据」
     *
     * 大致步骤：
     * 「1」先反射获取「PipelineDraweeController # Supplier<DataSource<CloseableReference<CloseableImage>>>
     * mDataSourceSupplier
     * mDataSourceSupplier」
     * 「2」com.facebook.drawee.controller.AbstractDraweeControllerBuilder$2「这个就有意思了」
     * 在「Fresco 1.4.0」代码的面板中，顺位第 2 个创建的 匿名内部类是 new Supplier<DataSource<IMAGE>>(){...}
     * 也就是说反射这个拿 Supplier<DataSource<IMAGE>>，而这个 Supplier<DataSource<IMAGE>> 中的泛型
     * IMAGE 的 field 就是 下面的 mAutoField
     *
     * @param draweeView draweeView
     * @return PipelineDraweeControllerBuilder
     */
    private PipelineDraweeControllerBuilder getFrescoControllerBuilder(DraweeView draweeView) {
        try {
            PipelineDraweeController controller
                = (PipelineDraweeController) draweeView.getController();
            Field mDataSourceSupplierFiled = PipelineDraweeController.class.getDeclaredField(
                "mDataSourceSupplier");
            mDataSourceSupplierFiled.setAccessible(true);
            Supplier supplier = (Supplier) mDataSourceSupplierFiled.get(controller);
            Field mAutoField = Class.forName(
                "com.facebook.drawee.controller.AbstractDraweeControllerBuilder$2")
                .getDeclaredField("this$0");
            mAutoField.setAccessible(true);
            PipelineDraweeControllerBuilder builder
                = (PipelineDraweeControllerBuilder) mAutoField.get(supplier);
            return builder;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 获取 bitmap
     * 兼容了 「Fresco bitmap」的情况
     *
     * @param drawable drawable
     * @return bitmap
     */
    private Bitmap getFrescoDrawableBitmap(Drawable drawable) {
        try {
            if (drawable instanceof ScaleTypeDrawable) {
                return ((BitmapDrawable) drawable.getCurrent()).getBitmap();
            } else if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
