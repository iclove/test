package org.lanqiao;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedList;

public class ConnectionUtil {

	// 创建一个连接池
	private static LinkedList<Connection> pool = new LinkedList<Connection>();
	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			// 创建连接
			for (int i = 0; i < 3; i++) {
				final Connection conn = DriverManager.getConnection(
						"jdbc:mysql:///test", "root", "root");
				// 通过动态代理增强close方法-- 通过Proxy
				Object objProxyed = Proxy.newProxyInstance(
						ConnectionUtil.class.getClassLoader(), // 目标类的类加载器
						new Class[] { Connection.class }, // 目标类和代理类必须实现同一个接口
						new InvocationHandler() {// 代理目标类所有方法
							@Override
							public Object invoke(Object proxy, Method method,
									Object[] args) throws Throwable {
								// 判断是否是目标方法
								if (method.getName().equals("close")) {
									synchronized (pool) {
										pool.addLast((Connection) proxy);// 当关闭连接时，增强close方法，将连接重新加回到池中
										pool.notify();
									}
									return null;
								} else {// 非目标方法，放行
									return method.invoke(conn, args);
								}
							}
						});
				pool.add((Connection) objProxyed);// 将经过close方法增强的连接放到池中
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	/**
	 * <p>
	 * Title: getConn
	 * </p>
	 * <p>
	 * Description: 声明静态工厂方法返回连接
	 * </p>
	 * 
	 * @return
	 */
	public static Connection getConn() {
		Connection conn = null;
		synchronized (pool) {
			if (pool.size() == 0) {
				try {
					pool.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return getConn();
			}
			Connection con = pool.removeFirst();// 返回一个代理的connection对象
			System.err.println("还有几个:" + pool.size());
			return con;
		}
	}

	public static void main(String[] args) {
		System.err.println(getConn());
	}

}
