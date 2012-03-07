package com.dianping.cat.job.sql;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import com.dianping.cat.job.mapreduce.MessageTreeWritable;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.spi.MessageTree;

public class SqlJobMapper extends Mapper<Object, MessageTreeWritable, SqlStatementKey, SqlStatementValue> {

	public static final String DEFAULT_DOMAIN = "NoDomain";

	private void handle(Context context, MessageTree tree) throws IOException, InterruptedException {
		Message message = tree.getMessage();
		String domain = tree.getDomain();
		if (domain == null || domain.length() == 0) {
			domain = DEFAULT_DOMAIN;
		}

		if (message instanceof Transaction) {
			Transaction transaction = (Transaction) message;

			processTransaction(context, transaction, tree, domain);
		}
	}

	public void map(Object key, MessageTreeWritable value, Context context) throws IOException, InterruptedException {
		MessageTree message = value.get();
		handle(context, message);
	}

	private void processTransaction(Context context, Transaction transaction, MessageTree tree, String domain)
	      throws IOException, InterruptedException {
		String type = transaction.getType();

		if (type.equals("SQL")) {
			SqlStatementKey statementKey = new SqlStatementKey();
			String statement = transaction.getName();
			long duration = transaction.getDuration();
			int flag = 0;

			statementKey.setDomain(new Text(domain)).setStatement(new Text(statement));
			if (!transaction.getStatus().equals(Transaction.SUCCESS)) {
				flag = 1;
			}
			SqlStatementValue result = new SqlStatementValue(flag, duration);
			context.write(statementKey, result);
		}

		List<Message> messageList = transaction.getChildren();

		for (Message message : messageList) {
			if (message instanceof Transaction) {
				Transaction temp = (Transaction) message;

				processTransaction(context, temp, tree, domain);
			}
		}
	}
}