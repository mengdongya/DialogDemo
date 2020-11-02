package cn.mdy.platform

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.FrameLayout
import androidx.annotation.NonNull
import androidx.annotation.StyleRes
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * 作者： mdy
 * 日期： 2020/11/2
 * 描述:
 */
abstract class BaseDialog : DialogFragment(), DialogInterface {

    private lateinit var activity: Activity
    private lateinit var rootView: FrameLayout
    private var onCancelListener: DialogInterface.OnCancelListener? = null
    private var onDismissListener: DialogInterface.OnDismissListener? = null
//    protected var coroutine: CoroutineSupport? = null

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        this.activity = activity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//        coroutine = CoroutineSupport()
        rootView = FrameLayout(activity)
        val resId = getContentViewId()
        if (resId > 0) {
            var contentView: View? = null
            try {
                contentView = inflater.inflate(resId, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            contentView?.let {
                rootView.addView(it)
            }
        }
        return rootView
    }

    abstract fun getContentViewId(): Int

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return if (useBottomSheet()) BottomSheetDialog(activity, theme) else super.onCreateDialog(savedInstanceState)
    }

    override fun show(manager: androidx.fragment.app.FragmentManager, tag: String?) {
        try {
            if (isShowing()) return
            super.show(manager, tag)
        } catch (e: Exception) {
            try {
                val ft = manager.beginTransaction()
                ft.add(this, tag)
                ft.commitAllowingStateLoss()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    open fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
        show(fragmentManager, javaClass.name)
    }

    override fun cancel() {
        dialog?.cancel()
    }

    fun isShowing(): Boolean {
        return dialog != null && dialog?.isShowing == true
    }

    /**
     * 是否使用bottom sheet
     * @return false
     */
    open fun useBottomSheet(): Boolean {
        return false
    }

    fun setCanceledOnTouchOutside(isCancelable: Boolean = true) {
        dialog?.setCanceledOnTouchOutside(isCancelable)
    }

    override fun onResume() {
        super.onResume()
        setDialogWidth()
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        coroutine?.destroy()
    }

    /**
     * 设置对话框宽度，可以在子类的 onResume 中重新设置
     * @param scale 相对于屏幕宽度的比例，默认 0.8
     */
    private fun setDialogWidth(scale: Float = 0.8f) {
        if (useBottomSheet()) {
            return
        }
        val window = dialog?.window ?: return
        // 设置背景透明
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val width = if (scale >= 1) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            val screenWidth = resources.displayMetrics.widthPixels
            (screenWidth * scale).toInt()
        }
        window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    /**
     * 设置对话框宽高为 MatchParent
     */
    fun setDialogMatchParent() {
        if (useBottomSheet()) {
            return
        }
        val window = dialog?.window ?: return
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    /**
     * 设置bottomSheet最小显示高度  必须在[.onActivityCreated]或之后 调用
     * @param heightPixels heightPixels
     */
    fun setBottomSheetPeekHeight(heightPixels: Int) {
        val view = view ?: return
        val layoutParams = (view.parent as FrameLayout).layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior as BottomSheetBehavior?
        behavior?.let {
            it.peekHeight = heightPixels
        }
    }

    /**
     * 设置 bottomSheet 不能拖拽关闭，并且点击空白区域关闭
     */
    fun setBottomSheetTouchCancelable() {
        if (!useBottomSheet()) {
            return
        }
        val view = view ?: return
        val layoutParams = (view.parent as FrameLayout).layoutParams as androidx.coordinatorlayout.widget.CoordinatorLayout.LayoutParams
        val behavior = layoutParams.behavior as BottomSheetBehavior?
        behavior?.setBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(@NonNull bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_DRAGGING) {
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            override fun onSlide(@NonNull bottomSheet: View, slideOffset: Float) = Unit
        })
    }

    /**
     * 设置转场动画
     */
    fun setDialogAnim(@StyleRes resId: Int) {
        dialog?.window?.setWindowAnimations(resId)
    }

    /**
     * R.style.hitch_dialog_theme 如果需要自定义背景 设置这个theme
     */
    override fun getTheme(): Int {
        return if (useBottomSheet()) super.getTheme() else R.style.AppTheme_Dialog_Base_Default
    }

    /**
     * 设置dialog位置
     *
     * @param gravity 位置
     */
    private fun setDialogGravity(gravity: Int = Gravity.CENTER) {
        dialog?.window?.setGravity(gravity)
    }

    /**
     * 设置dialog靠下显示
     */
    fun setDialogBottom() {
        setDialogGravity(Gravity.BOTTOM)
    }

    fun showInputMethod() {
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener) {
        this.onCancelListener = onCancelListener
    }

    fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener) {
        this.onDismissListener = onDismissListener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.onDismiss(this)
//        coroutine?.cancelChildren()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onCancelListener?.onCancel(this)
    }

    // protected
    private fun isActivityFinish(): Boolean {
        return activity.isFinishing
    }

    protected fun toast(msg: String) {
        if (!::activity.isInitialized) {
            return
        }
        if (isActivityFinish()) return
        if (TextUtils.isEmpty(msg)) return
        try {
//            HMUIToast.toast(activity, msg)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun dismiss() {
        try {
            super.dismiss()
        } catch (e: Exception) {
            dismissAllowingStateLoss()
        }
    }
}
