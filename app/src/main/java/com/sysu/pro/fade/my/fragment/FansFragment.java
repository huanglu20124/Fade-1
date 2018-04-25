package com.sysu.pro.fade.my.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadmoreListener;
import com.sysu.pro.fade.Const;
import com.sysu.pro.fade.R;
import com.sysu.pro.fade.beans.User;
import com.sysu.pro.fade.beans.UserQuery;
import com.sysu.pro.fade.home.activity.OtherActivity;
import com.sysu.pro.fade.home.adapter.CommonAdapter;
import com.sysu.pro.fade.service.UserService;
import com.sysu.pro.fade.utils.RetrofitUtil;
import com.sysu.pro.fade.utils.UserUtil;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Retrofit;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class FansFragment extends Fragment {

    private User myself;
    private Integer userId;
    private String start;
    private View rootView;
    private Retrofit retrofit;
    private RefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private CommonAdapter<User> adapter;
    private List<User> fans = new ArrayList<>();

    public FansFragment() {
        // Required empty public constructor
    }

    public static FansFragment newInstance(int userId) {
        final FansFragment f = new FansFragment();
        final Bundle args = new Bundle();
        args.putInt("USER_ID", userId);
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_forward, container, false);
        myself = new UserUtil(getActivity()).getUer();
        userId = getArguments() != null ? getArguments().getInt("USER_ID") : null;
        start = "0";
        retrofit = RetrofitUtil.createRetrofit(Const.BASE_IP, myself.getTokenModel());
        setupView();
        getData();
        return rootView;
    }

    private void getData() {
        UserService service = retrofit.create(UserService.class);
        service.getFans(userId.toString(),myself.getUser_id().toString(), start)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<UserQuery>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("GetFansErr", "onError: " + e.toString());
                    }

                    @Override
                    public void onNext(UserQuery userQuery) {
                        refreshLayout.finishLoadmore();
                        fans.addAll(userQuery.getList());
                        Log.d("Check", "fans: "+userQuery.getList().size());
                        start = userQuery.getStart().toString();
                        adapter.notifyDataSetChanged();
                        if (fans.size() % 20 != 0) {
                            refreshLayout.setEnableLoadmore(false);
                        } else {
                            refreshLayout.setEnableLoadmore(true);
                        }
                    }
                });
    }

    private void setupView() {
        recyclerView = rootView.findViewById(R.id.fragment_forward_recycler_view);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.fans_gray));
        adapter = new CommonAdapter<User>(fans) {
            @Override
            public int getLayoutId(int ViewType) {
                return R.layout.item_fans_fragment;
            }

            @Override
            public void convert(ViewHolder holder, final User data, int position) {
                if (position == 0) {
                    holder.setWidgetVisibility(R.id.fans_divide_line, View.GONE);
                }
                holder.setCircleImage(R.id.fans_head, Const.BASE_IP+data.getHead_image_url());
                holder.onWidgetClick(R.id.fans_root_view, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getContext(), OtherActivity.class);
                        i.putExtra(Const.USER_ID, data.getUser_id());
                        startActivity(i);
                    }
                });
                holder.setText(R.id.fans_name, data.getNickname());
                holder.setText(R.id.fans_signature, data.getSummary());
                //如果打开别人的页面，粉丝是自己，就不用显示是否关注了
                if (data.getUser_id() != userId) {
                    if (data.getIsConcern() == 1) {
                        holder.setWidgetVisibility(R.id.fans_concern_ok, View.VISIBLE);
                    } else {
                        holder.setWidgetVisibility(R.id.fans_concern, View.VISIBLE);
                    }
                }
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        refreshLayout = rootView.findViewById(R.id.fragment_forward_refresh_layout);
        refreshLayout.setEnableRefresh(false);
        refreshLayout.setEnableAutoLoadmore(false);
        refreshLayout.setOnLoadmoreListener(new OnLoadmoreListener() {
            @Override
            public void onLoadmore(RefreshLayout refreshlayout) {
                getData();
            }
        });
    }

}
