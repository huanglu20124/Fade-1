package com.sysu.pro.fade.home.listener;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.sysu.pro.fade.Const;
import com.sysu.pro.fade.beans.Note;

import java.util.Date;
import java.util.List;

/**
 * Created by LaiXiancheng on 2017/9/14.
 * Email: lxc.sysu@qq.com
 * 监听滑动状态以判断item的删除时机
 * item的删除条件也在这个类里面
 */

public class JudgeRemoveOnScrollListener extends RecyclerView.OnScrollListener {
	String logTag = "scroll";
	private Context context;
	private List<Note> notes;
	private List<Integer>now_note_id;



	public JudgeRemoveOnScrollListener(Context context
			, List<Note> notes, List<Integer> now_note_id) {
		this.notes = notes;
		this.context = context;
		this.now_note_id = now_note_id;
	}


	@Override
	public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
		super.onScrollStateChanged(recyclerView, newState);
		if (newState == RecyclerView.SCROLL_STATE_IDLE) {
			judgeAndRemoveItem(recyclerView);
		}
	}

	/**
	 * 移除当前可视的item中满足移除条件的item
	 *
	 * @param recyclerView
	 */
	public void judgeAndRemoveItem(RecyclerView recyclerView) {
		LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
		int firstVisible = layoutManager.findFirstVisibleItemPosition();
		int lastVisible = layoutManager.findLastVisibleItemPosition();
		if (firstVisible < lastVisible)
			for (int i = firstVisible; i <= lastVisible; i++) {
				if (i < notes.size() && matchRemoveCondition(i)) {
					removeItem(recyclerView, i);
				}
			}
		else if (lastVisible < notes.size() && matchRemoveCondition(lastVisible)) {
			removeItem(recyclerView, lastVisible);
		}
	}

	/**
	 * 判断item是否满足移除条件
	 *
	 * @param i item在recycleView中的位置
	 * @return 是否满足移除条件
	 */
	private boolean matchRemoveCondition(int i) {
		Note bean = notes.get(i);
		Date dateNow = new Date(bean.getFetchTime());
		Date datePost = bean.getPost_time();
		//floor是为了防止最后半秒的计算结果就为0,也就是保证了时间真正耗尽之后计算结果才为0
		long minuteLeft = (long) (Const.HOME_NODE_DEFAULT_LIFE + 5 * bean.getGood_num()
				- Math.floor(((double) (dateNow.getTime() - datePost.getTime())) / (1000 * 60)));
		return minuteLeft <= 0;
	}

	/**
	 * 移除recycleView中position位置的item，item需要至少有一半是可见的才能移除成功
	 *
	 * @param recyclerView item所在的recycleView
	 * @param position     item在recycleView中的位置
	 */
	private void removeItem(final RecyclerView recyclerView, int position) {
		Log.d("heightTest", String.format("measure = %d   no = %d", recyclerView.getMeasuredHeight(), recyclerView.getHeight()));
		LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
		int firstVisible = layoutManager.findFirstVisibleItemPosition();
		int lastVisible = layoutManager.findLastVisibleItemPosition();
		View itemView;
		//确保要移除的view是可见的，同时保证了这个view没被回收利用
		if (position < firstVisible || position > lastVisible)
			return;
		else
			itemView = layoutManager.findViewByPosition(position);

		//view的中间点Y坐标在可视范围内，保证了要移除的view至少有一半是可见的才能移除成功
		float middleY = itemView.getTop() + itemView.getHeight() / 2;
		if (middleY < 0 || middleY > recyclerView.getHeight())
			return;
		notes.remove(position);
		now_note_id.remove(position);
		recyclerView.getAdapter().notifyItemRemoved(position);

		//移除之后，会有新的item填充进来，填充进来之后要判断是否移除，条件成立则移除。这样就变成了递归移除
		//这里设置1.5秒是为了等前一个的消失动画执行完
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				((Activity) context).runOnUiThread(new Runnable() {
					@Override
					public void run() {
						judgeAndRemoveItem(recyclerView);
					}
				});

			}
		}).start();
	}

}