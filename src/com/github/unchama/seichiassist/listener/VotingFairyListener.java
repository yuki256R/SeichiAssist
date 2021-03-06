package com.github.unchama.seichiassist.listener;

import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.github.unchama.seichiassist.SeichiAssist;
import com.github.unchama.seichiassist.data.Mana;
import com.github.unchama.seichiassist.data.PlayerData;
import com.github.unchama.seichiassist.task.VotingFairyTaskRunnable;
import com.github.unchama.seichiassist.util.Util;

public class VotingFairyListener implements Listener {

	public static void summon(Player p){
		HashMap<UUID,PlayerData> playermap = SeichiAssist.playermap;
		UUID uuid = p.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);
		Mana mana = playerdata.activeskilldata.mana;

		//召喚した時間を取り出す
		playerdata.VotingFairyStartTime = new GregorianCalendar(
				Calendar.getInstance().get(Calendar.YEAR),
				Calendar.getInstance().get(Calendar.MONTH),
				Calendar.getInstance().get(Calendar.DATE),
				Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
				Calendar.getInstance().get(Calendar.MINUTE)
				);

		int min = Calendar.getInstance().get(Calendar.MINUTE) + 1,
			hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

		min = (playerdata.toggleVotingFairy % 2) != 0 ? min + 30 : min;
		hour = playerdata.toggleVotingFairy == 2 ? hour + 1
			 : playerdata.toggleVotingFairy == 3 ? hour + 1
			 : playerdata.toggleVotingFairy == 4 ? hour + 2
					 : hour;

		playerdata.VotingFairyEndTime = new GregorianCalendar(
				Calendar.getInstance().get(Calendar.YEAR),
				Calendar.getInstance().get(Calendar.MONTH),
				Calendar.getInstance().get(Calendar.DATE),
				hour,
				min
				);

		//投票ptを減らす
		playerdata.activeskilldata.effectpoint -= playerdata.toggleVotingFairy*2;
		//フラグ
		playerdata.usingVotingFairy = true;

		//マナ回復量最大値の決定
		double n = mana.getMax();
		playerdata.VotingFairyRecoveryValue = (int) ((n/10-n/30+(new Random().nextInt((int) (n/20))))/2.9) + 200;

		p.sendMessage(ChatColor.RESET + "" + ChatColor.YELLOW + "" + ChatColor.BOLD + "妖精を呼び出しました！");
		p.sendMessage(ChatColor.RESET + "" + ChatColor.YELLOW + "" + ChatColor.BOLD + "この子は1分間に約" + playerdata.VotingFairyRecoveryValue + "マナ");
		p.sendMessage(ChatColor.RESET + "" + ChatColor.YELLOW + "" + ChatColor.BOLD + "回復させる力を持っているようです。");

		//メッセージ
		final List<String> morning = Arrays.asList(
				"おはよ！[str1]","ヤッホー[str1]！"
				,"ふわぁ。。。[str1]の朝は早いね。"
				,"うーん、今日も一日頑張ろ！"
				,"今日は整地日和だね！[str1]！"
				);
		final List<String> day = Arrays.asList(
				"やあ！[str1]","ヤッホー[str1]！"
				,"あっ、[str1]じゃん。丁度お腹空いてたんだ！"
				,"この匂い…[str1]ってがちゃりんごいっぱい持ってる…?"
				,"今日のおやつはがちゃりんごいっぱいだ！"
				);
		final List<String> night = Arrays.asList(
				"やあ！[str1]","ヤッホー[str1]！"
				,"ふわぁ。。。[str1]は夜も元気だね。"
				,"もう寝ようと思ってたのにー。[str1]はしょうがないなぁ"
				,"こんな時間に呼ぶなんて…りんごははずんでもらうよ？"
				);

		if (Util.getTimeZone(playerdata.VotingFairyStartTime) == "morning")
			VotingFairyTaskRunnable.speak(p, getMessage(morning, p.getName()), playerdata.toggleVFSound);
		else if (Util.getTimeZone(playerdata.VotingFairyStartTime) == "day")
			VotingFairyTaskRunnable.speak(p, getMessage(day, p.getName()), playerdata.toggleVFSound);
		else
			VotingFairyTaskRunnable.speak(p, getMessage(night, p.getName()), playerdata.toggleVFSound);
	}

	public static void regeneMana(Player p) {
		HashMap<UUID,PlayerData> playermap = SeichiAssist.playermap;
		UUID uuid = p.getUniqueId();
		PlayerData playerdata = playermap.get(uuid);
		Mana mana = playerdata.activeskilldata.mana;

		if (mana.getMana() == mana.getMax()) {		//マナが最大だった場合はメッセージを送信して終わり
			final List<String> msg = Arrays.asList(
					"整地しないのー？"
					,"たくさん働いて、たくさんりんごを食べようね！"
					,"僕はいつか大きながちゃりんごを食べ尽して見せるっ！"
					,"ちょっと食べ疲れちゃった"
					,"[str1]はどのりんごが好き？僕はがちゃりんご！"
					,"動いてお腹を空かしていっぱい食べるぞー！"
					);
			VotingFairyTaskRunnable.speak(p, getMessage(msg, p.getName()), playerdata.toggleVFSound);

		}else {

			double n = playerdata.VotingFairyRecoveryValue;	//実際のマナ回復量
			int	m = getGiveAppleValue(playerdata);			//りんご消費量

			//連続投票によってりんご消費量を抑える
			if (playerdata.ChainVote >= 30 )		m /= 2;
			else if (playerdata.ChainVote >= 10 )	m /= 1.5;
			else if (playerdata.ChainVote >= 3 )	m /= 1.25;

			//トグルで数値変更
			if (playerdata.toggleGiveApple == 2) {
				if (mana.getMana()/mana.getMax() >= 0.75) {
					n /= 2;
					m /= 2;
				}
			}else if (playerdata.toggleGiveApple == 3) {
				n /= 2;
				m /= 2;
			}

			if(m == 0) m = 1;

			if (playerdata.toggleGiveApple == 4) {
				n /= 4;
				m = 0;
			}else {	//ちょっとつまみ食いする
				m += new Random().nextInt(m/10);
			}

			//りんご所持数で値変更
			int gachaimo_minestack_num = Util.MineStackobjname_indexOf("gachaimo");
			int l = playerdata.minestack.getNum(gachaimo_minestack_num);

			if(m > l) {
				if (l == 0){
					n /= 2;
					if (playerdata.toggleGiveApple == 1) n /= 2;
					if (playerdata.toggleGiveApple == 2 && (mana.getMana()/mana.getMax() < 0.75)) n /= 2;
					p.sendMessage(ChatColor.RESET + "" + ChatColor.YELLOW + "" + ChatColor.BOLD + "MineStackにがちゃりんごがないようです。。。");
				}else {
					double M = m,L = l;
					n = ((L/M) <= 0.5 ? n * 0.5 : (n * L/M));
				}
				m = l;
			}

			//回復量に若干乱数をつける
			n = (n - n/100) + new Random().nextInt((int) (n/50));


			//マナ回復
			mana.increaseMana(n, p, playerdata.level);
			//りんごを減らす
			playerdata.minestack.setNum(Util.MineStackobjname_indexOf("gachaimo"), playerdata.minestack.getNum(Util.MineStackobjname_indexOf("gachaimo")) - m);
			//減ったりんごの数をplayerdataに加算
			playerdata.p_apple += m;

			//メッセージ
			final List<String> yes = Arrays.asList(
					"(´～｀)ﾓｸﾞﾓｸﾞ…"
					,"がちゃりんごって美味しいよね！"
					,"あぁ！幸せ！"
					,"[str1]のりんごはおいしいなぁ"
					,"いつもりんごをありがとう！"
					);
			final List<String> no = Arrays.asList(
					"お腹空いたなぁー。"
					,"がちゃりんごがっ！食べたいっ！"
					,"(´；ω；`)ｳｩｩ ﾋﾓｼﾞｲ..."
					,"＠うんちゃま [str1]が意地悪するんだっ！"
					,"うわーん！お腹空いたよー！"
					);

			p.sendMessage(ChatColor.RESET + "" + ChatColor.YELLOW + "" + ChatColor.BOLD + "マナ妖精が" + (int) n + "マナを回復してくれました");
			if (m != 0) {
				p.sendMessage(ChatColor.RESET + "" + ChatColor.YELLOW + "" + ChatColor.BOLD + "あっ！" + m + "個のがちゃりんごが食べられてる！");
				VotingFairyTaskRunnable.speak(p, getMessage(yes, p.getName()), playerdata.toggleVFSound);
			}else {

				p.sendMessage(ChatColor.RESET + "" + ChatColor.YELLOW + "" + ChatColor.BOLD + "あなたは妖精にりんごを渡しませんでした。");
				VotingFairyTaskRunnable.speak(p, getMessage(no, p.getName()), playerdata.toggleVFSound);
			}
		}

	}

	private static int getGiveAppleValue(PlayerData playerdata) {
		int i = playerdata.level/10;
		//0になるなら1を返す
		return i*i <= 0 ? 1 : i*i ;
	}

	private static String getMessage(List<String> messages, String str1) {
		String msg = messages.get(new Random().nextInt(messages.size()));
		if (!str1.isEmpty()) {
			msg = msg.replace("[str1]", str1 + ChatColor.RESET);
		}
		return msg;
	}
}