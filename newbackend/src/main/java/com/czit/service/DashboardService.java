package com.czit.service;

import com.czit.vo.DashboardVO;

/**
 * 数据看板聚合接口。
 *
 * <p>实现见 {@link com.czit.impl.DashboardServiceImpl}。</p>
 */
public interface DashboardService {

    DashboardVO dashboard();
}
