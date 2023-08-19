/*
 * Copyright (C) 2023 Frank Miles - Frms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package psnl.frms.kmao.decode

import android.util.Base64
import cn.hutool.core.io.FileUtil
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Arrays
import java.util.PriorityQueue
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


/**
 *
 * @author Frms(Frank Miles)
 * @email 3505826836@qq.com
 * @time 2023/08/19 下午 11:38
 */
fun check(input: String, output: String): Pair<StringBuilder, Boolean>
{
	var flag = true
	val stringBuilder = StringBuilder()
	val inputFile = File(input)
	
	
	if(inputFile.canRead().not()) {
		flag = false
		stringBuilder.append("输入文件夹：\n").append(input).append("\n不可读！")
	} else if(inputFile.isDirectory.not()) {
		flag = false
		stringBuilder.append("输入路径：\n").append(input).append("\n不是文件夹！")
	}
	
	if(flag && stringBuilder.isNotEmpty()) {
		stringBuilder.append('\n')
	}
	
	val outputFile = File(output)
	
//	if(outputFile.canRead().not()) {
//		flag = false
//		stringBuilder.append("输出文件夹：\n").append(output).append("\n不可读！")
//	} else
	if(outputFile.isFile) {
		flag = false
		stringBuilder.append("输出文件夹：\n").append(output).append("\n已经存在了！")
	}
	if(flag) {
		stringBuilder.append("输出、输出文件填写正确，可以生成了！")
	}
	return stringBuilder to flag
}

@Throws(Exception::class)
@Synchronized
fun decrypt(str: String, key : String, option:String): ByteArray
{
	return try
	{
		val decode = Base64.decode(str.toByteArray(charset("UTF-8")), 0)
		val ivParameterSpec = IvParameterSpec(decode, 0, 16)
		val copyOfRange = Arrays.copyOfRange(decode, 16, decode.size)
		val cipher = Cipher.getInstance(option)
		cipher.init(2, SecretKeySpec(key.toByteArray(), "AES"), ivParameterSpec)
		cipher.doFinal(copyOfRange)
	} catch (unused: Exception) { ByteArray(size = 0) }
}

fun buildNovel(input: File, output: File, key : String, option:String): String
{
	var message = ""
	var count = 1
	val str = StringBuffer()
	val executorService = Executors.newSingleThreadExecutor()
	val queue = PriorityQueue { a: File, b: File ->
		a.name.compareTo(b.name)
	}
	input.listFiles()?.let { queue.addAll(it) }
	
	while (queue.isNotEmpty())
	{
		val poll = queue.poll()!!
		executorService.submit {
			val f = FileUtil.readLines(
					poll,
					StandardCharsets.UTF_8
			)
			val stringBuilder = java.lang.StringBuilder()
			f.forEach(stringBuilder::append)
			
			val p: ByteArray = decrypt(stringBuilder.toString(), key, option)
			if (p.isEmpty()) {
				message += ("${poll.absoluteFile.name}   无法读取，错误文件！\n")
			} else
			{
				str.append('第').append(count).append("章\n").append(String(p)).append('\n')
				count++
			}
		}
	}
	
	executorService.shutdown();
	
	while (true)
	{
		if(executorService.isTerminated)
		{
			FileUtil.writeString(str.toString(), output, StandardCharsets.UTF_8);
			break;
		}
	}
	message += ("结束！已经写入章节数"+(count-1));
	return message
}