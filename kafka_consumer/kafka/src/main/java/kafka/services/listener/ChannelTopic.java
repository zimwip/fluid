/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kafka.services.listener;

import org.springframework.util.Assert;
/**
 *
 * @author tzimmer
 */
public class ChannelTopic  implements Topic {

	private final String channelName;

	/**
	 * Constructs a new {@link ChannelTopic} instance.
	 *
	 * @param name must not be {@literal null}.
	 */
	public ChannelTopic(String name) {

		Assert.notNull(name, "Topic name must not be null!");

		this.channelName = name;
	}

	/**
	 * @return topic name.
	 */
	@Override
	public String getTopic() {
		return channelName;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return channelName;
	}
}
